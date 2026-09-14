# AI Agent 开发架构梳理（ai-service + rag-service）

> 面试复习文档：按 AI Agent 开发完整流程，梳理两个 Python 服务的每个模块对应的核心知识点与关键代码。

## 项目概览

两个服务构成一个 **Agentic RAG 系统**：

| 服务 | 定位 | 技术栈 |
|---|---|---|
| `ai-service` | 垂直医疗 Agent（辨证论治 + 业务闭环） | FastAPI + LangChain 0.3（AgentExecutor）+ pymilvus |
| `rag-service` | 通用文档知识库 + RAG 问答 | FastAPI + LangChain 1.x（LCEL）+ langchain-milvus |
| `shared/` | 两服务共用的 embedding 基础设施 | httpx + DashScope text-embedding-v4 |

---

## 完整目录结构

```
ai-service/
├── app/
│   ├── agents/           # Agent 核心：llm / tools / schemas / prompt / agent
│   ├── ingest/           # 古籍入库：loader / chunker / pipeline
│   ├── vector/           # 向量：client(Milvus) / embedder
│   ├── rules/            # 规则引擎：十八反十九畏
│   ├── services/         # 业务编排：diagnosis_service / pdf_service
│   ├── db/               # 数据库：engine / schema / read_sql / diagnosis_repo
│   ├── api/              # 接口：deps(JWT) / routes(diagnoses, knowledge)
│   ├── core/             # 基础：security / response / errors
│   ├── config.py         # pydantic-settings
│   └── main.py           # FastAPI 入口 + lifespan
├── data/classics/        # 古籍样例 JSON
└── Dockerfile

rag-service/
└── app/
    ├── routers/          # knowledge_bases / documents / query
    ├── services/         # document_service / rag_service / vector_store
    ├── container.py      # 依赖注入
    ├── database.py       # SQLite 元数据
    ├── models.py         # Pydantic
    ├── security.py       # JWT
    └── config.py         # Settings(env_prefix=RAG_ + validation_alias 统一)

shared/
└── embedding.py          # DashScope text-embedding-v4（batch 上限 10）
```

---

## AI Agent 开发流程对照（含核心代码）

### 1. 数据加载（Document Loading）

**知识点**：Document Loader——多格式文档统一转纯文本。

```python
# rag-service/app/services/document_service.py（节选）
def _parse(content: bytes, filename: str) -> str:
    ext = filename.rsplit(".", 1)[-1].lower()
    if ext in ("txt", "md"):   return content.decode("utf-8")
    if ext == "pdf":           return "\n".join(p.extract_text() for p in PdfReader(BytesIO(content)).pages)
    if ext == "docx":          return "\n".join(p.text for p in Document(BytesIO(content)).paragraphs)
```

### 2. 数据切片（Chunking）⭐ 领域感知切分

**知识点**：Text Splitter + 领域感知切分——古籍按「条文」切，而非通用递归切。

```python
# ai-service/app/ingest/chunker.py
@dataclass
class Chunk:
    book_title: str; chapter: str; chunk_index: int; source_text: str

def chunk_book(book: dict, max_chars=512, overlap=30) -> list[Chunk]:
    chunks, idx = [], 0
    for ch in book.get("chapters", []):
        for clause in ch.get("clauses", []):
            text = (clause.get("text") or "").strip()
            if len(text) <= max_chars:
                chunks.append(Chunk(book["book_title"], ch["chapter"], idx, text)); idx += 1
            else:
                for piece in _split_long(text, max_chars, overlap):
                    chunks.append(Chunk(book["book_title"], ch["chapter"], idx, piece)); idx += 1
    return chunks
```

### 3. 向量化（Embedding）

**知识点**：Embedding Model + OpenAI 兼容协议；为何不用 DeepSeek（不提供 embedding）；为何抽共享层。

```python
# shared/embedding.py
class DashScopeEmbedder:
    BATCH_SIZE = 10  # DashScope text-embedding-v4 单次 batch 上限
    def _embed(self, texts: list[str]) -> list[list[float]]:
        resp = httpx.post(self.url,
            headers={"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"},
            json={"model": self.model, "input": texts}, timeout=60.0)
        resp.raise_for_status()
        return [item["embedding"] for item in resp.json()["data"]]
    def embed_documents(self, texts):
        out = []
        for i in range(0, len(texts), self.BATCH_SIZE):
            out.extend(self._embed(texts[i:i+self.BATCH_SIZE]))
        return out
```

### 4. 向量存储（Vector Store）

**知识点**：Vector Database + Milvus——Collection / Field Schema / Index(HNSW) / metric(COSINE)。

```python
# ai-service/app/vector/client.py
schema = MilvusClient.create_schema(auto_id=True, enable_dynamic_field=False)
schema.add_field("id", DataType.INT64, is_primary=True)
schema.add_field("book_title", DataType.VARCHAR, max_length=256)
schema.add_field("source_text", DataType.VARCHAR, max_length=8192)
schema.add_field("float_vector", DataType.FLOAT_VECTOR, dim=1024)
index_params = MilvusClient.prepare_index_params()
index_params.add_index(field_name="float_vector", index_type="HNSW",
                       metric_type="COSINE", params={"M": 16, "efConstruction": 200})
client.create_collection("tcm_classics", schema=schema, index_params=index_params)
```

### 5. 检索（Retrieval）

**知识点**：语义检索 / 相似度计算（COSINE）。

```python
# ai-service/app/vector/client.py
def search(client, query_embedding, top_k=5):
    res = client.search(collection_name=COLLECTION, data=[query_embedding], limit=top_k,
        output_fields=["book_title", "chapter", "source_text"],
        search_params={"metric_type": "COSINE", "params": {"ef": 64}})
    return [{"book_title": h["entity"]["book_title"], "source_text": h["entity"]["source_text"],
             "score": h["distance"]} for h in res[0]]
```

### 6-7. 重排 / 混合检索 ⚠️ 目前缺失（可讲优化方向）

当前是纯向量检索。优化方向：`bge-reranker` 交叉编码重排 + BM25 稀疏检索 + RRF 融合。

### 8. 提示词工程（Prompt Engineering）

**知识点**：System Prompt 设计 + 硬性约束（强制工具调用、结构化输出、医学严谨）。

```python
# ai-service/app/agents/prompt.py（节选）
SYSTEM_PROMPT = """你是一名资深中医师助理，遵循「辨证论治」原则...
硬性要求（必须依次完成）：
- 必须先调用 search_classics 检索古籍，引用原文作为依据；
- 必须调用 get_patient_info / get_medical_records / get_prescription_history 结合病人实际；
- 必须调用 check_herb_compatibility 检查十八反十九畏；
- 必须调用 search_knowledge_base 检索现代药理规范，确保剂量符合规范；
- 剂量仅作建议，最终须由医生审核签字后生效。"""
```

### 9. Agent 编排 / 工具调用（Agent Orchestration）⭐ 核心

**知识点**：Function Calling / ReAct——「思考→调工具→观察→再思考」的循环。

```python
# ai-service/app/agents/agent.py
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

prompt = ChatPromptTemplate.from_messages([
    ("system", SYSTEM_PROMPT), ("human", "{input}"),
    MessagesPlaceholder(variable_name="agent_scratchpad"),
])
agent = create_tool_calling_agent(llm, tools, prompt)
executor = AgentExecutor(agent=agent, tools=tools,
                         return_intermediate_steps=True, max_iterations=12)
result = executor.invoke({"input": input_text})
# result["intermediate_steps"] → 完整工具调用轨迹
```

```python
# ai-service/app/agents/tools.py（工具示例）
@tool
def check_herb_compatibility(herbs: list[str]) -> str:
    """检查一组药材是否存在十八反十九畏配伍禁忌。"""
    conflicts = check_conflicts(herbs)
    return "无配伍禁忌。" if not conflicts else "发现配伍禁忌：" + "；".join(conflicts)
```

### 10. 模型调用（LLM Invocation）

**知识点**：LLM API + OpenAI 兼容协议（base_url 可切换任意兼容模型）。

```python
# ai-service/app/agents/llm.py
from langchain_openai import ChatOpenAI
def get_llm(temperature=0):
    return ChatOpenAI(model=settings.deepseek_model,      # deepseek-v4-pro
                      base_url=settings.deepseek_base_url, # https://api.deepseek.com/v1
                      api_key=settings.deepseek_api_key, temperature=temperature)
```

### 11. 结构化输出（Structured Output）

**知识点**：Pydantic 约束 + 结构化输出；为何不用 `with_structured_output`（deepseek-v4-pro 不支持 response_format）。

```python
# ai-service/app/agents/schemas.py
class HerbItem(BaseModel):
    herb_name: str; dosage_grams: float; notes: str | None = None
class Citation(BaseModel):
    book: str; chapter: str | None = None; source_text: str
class DiagnosisDraft(BaseModel):
    syndrome_pattern: str; treatment_principle: str; recommended_formula: str
    herbs: list[HerbItem]; usage_instructions: str; rationale: str
    classic_evidence: list[Citation] = []; cautions: list[str] = []

# ai-service/app/agents/agent.py（两阶段：工具循环 → 手动 JSON 解析）
def _parse_draft(text: str) -> DiagnosisDraft:
    text = re.sub(r"```(?:json)?\s*([\s\S]*?)```", r"\1", text.strip())
    start, end = text.find("{"), text.rfind("}")
    return DiagnosisDraft.model_validate_json(text[start:end+1])
```

### 12. 规则引擎 / 混合护栏（Guardrails）⭐ 医疗安全

**知识点**：LLM（提议）+ 确定性规则（校验）混合，可单测可审计。

```python
# ai-service/app/rules/shibafan_shijiuwei.py
SHIBA_FAN = [
    ({"甘草"}, {"甘遂", "大戟", "海藻", "芫花"}),
    (_WUTOU_GROUP, {"贝母", "瓜蒌", "半夏", "白蔹", "白及"}),
    ({"藜芦"}, {"人参", "沙参", "丹参", "玄参", "细辛", "芍药"}),
]
def check_conflicts(herbs: list[str]) -> list[str]:
    for a, b in itertools.combinations([normalize(h) for h in herbs], 2):
        for left, right in SHIBA_FAN:
            if (a in left and b in right) or (b in left and a in right):
                yield f"{a} 与 {b} 属十八反配伍禁忌"
```

### 13. RAG 问答链（RAG QA / LCEL）

**知识点**：Retrieval-Augmented Generation + LCEL 管道式声明链。

```python
# rag-service/app/services/rag_service.py
prompt = ChatPromptTemplate.from_messages([
    ("system", "{system_prompt}"),
    ("human", "以下是相关参考资料：\n\n{context}\n\n用户问题：{question}"),
])
self._chain = prompt | llm | StrOutputParser()
answer = self._chain.invoke({"system_prompt": prompt, "context": context, "question": question})
```

### 14. 可观测性（Observability）⭐ 可解释性

**知识点**：执行轨迹追踪 + 可解释性（能回答「AI 为什么这么开方」）。

```python
# ai-service/app/agents/agent.py
trace = []
for i, (action, observation) in enumerate(result.get("intermediate_steps", []), 1):
    trace.append(TraceStep(step=i, type="tool", tool_name=action.tool,
                           tool_input=str(action.tool_input), observation=str(observation)[:2000]))
# 轨迹落库到 ai_diagnoses.agent_trace，前端渲染时间线
```

### 15. 人机协同（Human-in-the-loop）⭐ 合规

**知识点**：AI 是副驾，草案必须医生签字才生效。

```python
# ai-service/app/services/diagnosis_service.py
def sign_diagnosis(user_id, diag_id, signature_text) -> bytes:
    d = get_diagnosis(user_id, diag_id)
    if d["status"] != "DRAFT": raise AppError(6003, "仅草稿状态可签字")
    pdf = pdf_service.generate_treatment_plan_pdf(d["draft_content"], ..., signature_text, ...)
    diagnosis_repo.sign_diagnosis(diag_id, user_id, signature_text)  # 置 SIGNED
    return pdf
```

### 16. 服务化与多语言协作（Serving & Polyglot）

**知识点**：FastAPI + REST + 跨语言微服务；PyJWT 验 Java 签发的 HS384 token。

```python
# ai-service/app/core/security.py
def decode_token(token: str) -> dict:
    # Java jjwt 依据 secret 长度自动选算法（58 字节 → HS384），兼容整个 HS 家族
    return jwt.decode(token, settings.jwt_secret, algorithms=["HS256", "HS384", "HS512"])
```

---

## 面试一句话总结

> 这是一个 **Agentic RAG 系统**：底层「加载→领域切片→向量化→Milvus→检索」是标准 RAG 管线；上层「工具调用 Agent + 规则引擎护栏 + 结构化输出 + 人机协同 + 轨迹可观测」构成垂直医疗应用。两服务共享 embedding 基础设施。

## 三个核心亮点

1. **领域感知切分**：条文切分 vs 通用递归切分，保留完整方义。
2. **真·工具调用 Agent + 轨迹可观测**：7 个工具自主编排，非单次 RAG。
3. **LLM + 规则引擎混合护栏**：十八反十九畏确定性校验，医疗安全。

## 两个诚实短板（可作优化方向）

- **重排（rerank）**：当前纯向量检索，未用 bge-reranker 二次精排。
- **混合检索（hybrid search）**：未做 BM25 稀疏检索 + 向量检索的 RRF 融合。
