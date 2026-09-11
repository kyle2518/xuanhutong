"""Agent 工具集：古籍检索 / 病人信息 / 病历 / 药材 / 经典方 / 配伍校验。

- 不依赖 user_id 的工具在模块级定义；
- 依赖 user_id 的（病人/病历）在 build_tools 内用闭包绑定，强制所有权约束。
"""
import httpx
import jwt
from langchain_core.tools import tool

from app.config import settings
from app.db.read_sql import (
    get_classic_prescription,
    get_herb,
    get_medical_records as query_medical_records,
    get_patient,
)
from app.rules.shibafan_shijiuwei import check_conflicts
from app.vector.client import get_client, search
from app.vector.embedder import get_embedder


def _format_hits(hits: list[dict]) -> str:
    if not hits:
        return "未检索到相关古籍条文。"
    lines = []
    for i, h in enumerate(hits, 1):
        chapter = h.get("chapter") or ""
        lines.append(
            f"[{i}]《{h['book_title']}》{chapter}：{h['source_text']}（相似度 {h.get('score', 0):.3f}）"
        )
    return "\n".join(lines)


def _search_knowledge_base(query: str, top_k: int, user_id: int) -> str:
    """调 rag-service 检索知识库中的现代规范/规章制度文档。"""
    token = jwt.encode({"sub": str(user_id)}, settings.jwt_secret, algorithm="HS256")
    try:
        resp = httpx.post(
            f"{settings.rag_base_url.rstrip('/')}/api/rag/search",
            headers={"Authorization": f"Bearer {token}"},
            json={"query": query, "top_k": top_k},
            timeout=30.0,
        )
        resp.raise_for_status()
        hits = resp.json().get("hits", [])
    except Exception as e:  # noqa: BLE001
        return f"知识库检索失败：{e}"
    if not hits:
        return "知识库中未检索到相关规范文档。"
    lines = []
    for i, h in enumerate(hits, 1):
        lines.append(
            f"[{i}] {h.get('source', '')}：{h.get('chunk', '')}（相似度 {h.get('score', 0)}）"
        )
    return "\n".join(lines)


@tool
def search_classics(query: str, top_k: int = 5) -> str:
    """检索中医古籍原文（《伤寒论》《金匮要略》等），返回相关条文与出处。"""
    embedder = get_embedder()
    q_emb = embedder.embed_query(query)
    hits = search(get_client(), q_emb, top_k)
    return _format_hits(hits)


@tool
def lookup_herb(name: str) -> str:
    """查询单味药材的性味归经、功效、主治、常用剂量、禁忌。"""
    h = get_herb(name)
    if not h:
        return f"未找到药材「{name}」。"
    return (
        f"药材：{h.get('chinese_name')}（{h.get('pinyin_name') or ''}）\n"
        f"性味：{h.get('properties') or '无'}；归经：{h.get('meridian_tropism') or '无'}\n"
        f"功效：{h.get('efficacy') or '无'}\n"
        f"主治：{h.get('indications') or '无'}\n"
        f"常用剂量：{h.get('dosage_range') or '无'}\n"
        f"禁忌：{h.get('contraindications') or '无'}"
    )


@tool
def lookup_classic_prescription(name: str) -> str:
    """查询经典方剂的组成、功效、主治、用法、出处。"""
    cp = get_classic_prescription(name)
    if not cp:
        return f"未找到经典方「{name}」。"
    return (
        f"方名：{cp.get('name')}（出处：{cp.get('source')}）\n"
        f"组成：{cp.get('composition')}\n"
        f"功效：{cp.get('efficacy') or '无'}\n"
        f"主治：{cp.get('indications') or '无'}\n"
        f"用法：{cp.get('usage_method') or '无'}"
    )


@tool
def check_herb_compatibility(herbs: list[str]) -> str:
    """检查一组药材是否存在十八反十九畏配伍禁忌，返回冲突对或"无配伍禁忌"。"""
    conflicts = check_conflicts(herbs)
    if not conflicts:
        return "无配伍禁忌（十八反十九畏）。"
    return "发现配伍禁忌：" + "；".join(conflicts)


def build_tools(user_id: int) -> list:
    @tool
    def get_patient_info(patient_id: int) -> str:
        """获取病人基本信息（姓名/性别/年龄/主诉）。"""
        p = get_patient(patient_id, user_id)
        if not p:
            return "未找到该病人（或无权访问）。"
        gender = {0: "男", 1: "女"}.get(p.get("gender"), "未知")
        return (
            f"姓名：{p.get('name')}；性别：{gender}；年龄：{p.get('age') or '未知'}；"
            f"主诉：{p.get('chief_complaint') or '无'}"
        )

    @tool
    def get_medical_records(patient_id: int, limit: int = 10) -> str:
        """获取病人近期病历（就诊日期/诊断/症状/治法）。"""
        records = query_medical_records(patient_id, user_id, limit)
        if not records:
            return "暂无病历记录。"
        lines = []
        for r in records:
            lines.append(
                f"- 就诊：{r.get('visit_date')}；诊断：{r.get('diagnosis') or '无'}；"
                f"症状：{r.get('symptoms') or '无'}；治法：{r.get('treatment_method') or '无'}"
            )
        return "\n".join(lines)

    @tool
    def search_knowledge_base(query: str, top_k: int = 5) -> str:
        """检索知识库中的现代药理规范、处方管理规定、国家规章制度等文档（如某味药的现代剂量限制、毒性禁忌）。"""
        return _search_knowledge_base(query, top_k, user_id)

    return [
        search_classics,
        get_patient_info,
        get_medical_records,
        lookup_herb,
        lookup_classic_prescription,
        check_herb_compatibility,
        search_knowledge_base,
    ]
