"""辨证论治 Agent：两阶段（工具调用研究循环 → 结构化输出）。"""
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

import json
import re

from app.agents.llm import get_llm
from app.agents.prompt import SYSTEM_PROMPT
from app.agents.schemas import DiagnosisDraft, TraceStep
from app.agents.tools import build_tools
from app.db.read_sql import get_patient
from app.rules.shibafan_shijiuwei import check_conflicts


def _parse_draft(text: str) -> DiagnosisDraft:
    """从 LLM 输出中提取 JSON 并解析为 DiagnosisDraft（剥离 markdown 代码块等）。"""
    text = str(text).strip()
    m = re.search(r"```(?:json)?\s*([\s\S]*?)```", text)
    if m:
        text = m.group(1).strip()
    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end != -1 and end > start:
        text = text[start : end + 1]
    return DiagnosisDraft.model_validate_json(text)


def run_diagnosis(user_id: int, patient_id: int, chief_complaint: str | None = None) -> dict:
    """执行一次 AI 辅助诊断，返回结构化草案 + 证据 + 轨迹 + 配伍警告。"""
    tools = build_tools(user_id)
    llm = get_llm()

    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", SYSTEM_PROMPT),
            ("human", "{input}"),
            MessagesPlaceholder(variable_name="agent_scratchpad"),
        ]
    )
    agent = create_tool_calling_agent(llm, tools, prompt)
    executor = AgentExecutor(
        agent=agent,
        tools=tools,
        return_intermediate_steps=True,
        max_iterations=12,
        verbose=False,
    )

    patient = get_patient(patient_id, user_id)
    complaint = chief_complaint or (patient.get("chief_complaint") if patient else "无")
    input_text = (
        f"请对以下病人进行中医辨证论治，并给出治疗草案。\n"
        f"病人ID：{patient_id}；主诉：{complaint}\n"
        f"请先检索古籍、获取病人信息与病历，再辨证、选方、组方，并校验配伍禁忌。"
    )

    result = executor.invoke({"input": input_text})

    # 执行轨迹：每次工具调用 + 观察结果
    trace: list[TraceStep] = []
    for i, (action, observation) in enumerate(result.get("intermediate_steps", []), 1):
        trace.append(
            TraceStep(
                step=i,
                type="tool",
                tool_name=action.tool,
                tool_input=str(action.tool_input),
                observation=str(observation)[:2000],
            )
        )

    research_output = result.get("output", "")

    # 第二阶段：让 LLM 输出 JSON 文本，手动解析（deepseek-v4-pro 不支持 response_format）
    finalize_prompt = (
        "以下是辨证论治的研究过程与结论，请将其整理为结构化的 JSON 治疗草案。\n\n"
        f"【研究结论】\n{research_output}\n\n"
        "请只输出一个 JSON 对象（不要输出任何其他文字或 markdown 代码块），字段如下：\n"
        '{"syndrome_pattern": "辨证", "treatment_principle": "治法", '
        '"recommended_formula": "推荐方剂名", "formula_source": "出处", '
        '"herbs": [{"herb_name": "药材", "dosage_grams": 0.0, "notes": "备注"}], '
        '"usage_instructions": "用药说明", "rationale": "方解依据", '
        '"classic_evidence": [{"book": "书名", "chapter": "篇章", "source_text": "原文"}], '
        '"cautions": ["注意事项"]}'
    )
    raw = llm.invoke(finalize_prompt)
    draft: DiagnosisDraft = _parse_draft(raw.content)

    # 确定性兜底：无论 Agent 是否调用过校验工具，都再跑一次规则引擎
    warnings = check_conflicts([h.herb_name for h in draft.herbs])

    return {
        "draft": draft.model_dump(),
        "evidence": [c.model_dump() for c in draft.classic_evidence],
        "agent_trace": [t.model_dump() for t in trace],
        "compatibility_warnings": warnings,
    }
