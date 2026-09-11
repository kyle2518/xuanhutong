"""Agent 结构化输出与执行轨迹的 Pydantic 模型。"""
from pydantic import BaseModel, Field


class HerbItem(BaseModel):
    herb_name: str = Field(description="药材名")
    dosage_grams: float = Field(description="剂量（克）")
    notes: str | None = Field(default=None, description="特殊用法/备注")


class Citation(BaseModel):
    book: str = Field(description="古籍书名")
    chapter: str | None = Field(default=None, description="篇章")
    source_text: str = Field(description="原文条文")


class DiagnosisDraft(BaseModel):
    syndrome_pattern: str = Field(description="辨证（证型）")
    treatment_principle: str = Field(description="治法")
    recommended_formula: str = Field(description="推荐方剂名")
    formula_source: str | None = Field(default=None, description="方剂出处")
    herbs: list[HerbItem] = Field(description="组成及剂量")
    usage_instructions: str = Field(description="用药说明/用法")
    rationale: str = Field(description="方解/辨证依据")
    classic_evidence: list[Citation] = Field(default_factory=list, description="古籍依据")
    cautions: list[str] = Field(default_factory=list, description="注意事项/配伍提示")


class TraceStep(BaseModel):
    step: int
    type: str = "tool"  # tool | final
    tool_name: str | None = None
    tool_input: str | None = None
    observation: str | None = None
