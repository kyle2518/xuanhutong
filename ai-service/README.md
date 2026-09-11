# AI 辅助诊断服务（Agentic RAG）

「悬壶通」中医门店管理系统的 AI 诊断微服务，**Python (FastAPI)** 实现，与现有 Java/Spring Cloud 微服务共存。

将古代中医典籍切片向量化存入 **Milvus**，构建可检索的知识库；医生发起诊断时，一个 **LangChain 工具调用 Agent** 结合病人年龄/病情/病历，多步推理产出**结构化治疗草案**，经医生 review、签字后生成正式治疗方案，并可「一键转处方」落回现有 `prescriptions` 表。

---

## 架构

```
医生(前端) ──> Java Gateway(:8080) ──/api/ai/**──> ai-service(FastAPI, :8085)
                                                     │
                          ┌──────────────────────────┼───────────────────────┐
                          ▼                          ▼                       ▼
                    LangChain Agent            Milvus(向量库)           MySQL(共享库)
                   (工具调用循环)            tcm_classics             patients/records/
                          │                (1024维 COSINE/HNSW)       herbs/classic_prescriptions
                          ├─ search_classics ──► bge-m3 embedding ──► Milvus
                          ├─ get_patient_info / get_medical_records ──► MySQL(按 user_id 隔离)
                          ├─ lookup_herb / lookup_classic_prescription ─► MySQL
                          └─ check_herb_compatibility ─► 十八反十九畏规则引擎
```

## 技术栈

- **FastAPI + Uvicorn**：异步 Web 框架，自动 OpenAPI 文档
- **LangChain**：`create_tool_calling_agent` + `AgentExecutor`（工具调用 Agent）
- **DeepSeek**（deepseek-chat，OpenAI 兼容）：推理大模型
- **SiliconFlow bge-m3**：Embedding（1024 维，OpenAI 兼容）
- **Milvus**（Standalone）：向量库，HNSW/COSINE 索引
- **SQLAlchemy + PyMySQL**：直连共享 MySQL（同步驱动，因 Agent 执行本质同步）
- **PyJWT**：复用 Java 侧签发的 HS256 token（`sub`=userId）
- **reportlab**：治疗方案 PDF 生成

## 核心设计

### 1. 领域感知切分（chunking）

古籍按「条文」为最小单元切分，而非通用递归字符切分——保留完整辨证语义。超长条文才按句读切分并带重叠。

### 2. 工具调用 Agent（非一次性 RAG）

6 个工具，Agent 自主决定调用哪些、迭代推理：

| 工具 | 作用 |
|---|---|
| `search_classics` | 向量检索古籍原文（RAG 证据） |
| `get_patient_info` | 病人信息（按 user_id 隔离） |
| `get_medical_records` | 近期病历 |
| `lookup_herb` | 药材性味归经/功效/禁忌 |
| `lookup_classic_prescription` | 经典方剂组成 |
| `check_herb_compatibility` | 十八反十九畏规则引擎 |

### 3. 两阶段结构化输出

1. `AgentExecutor` 跑工具循环，`return_intermediate_steps` 直接得到执行轨迹；
2. `llm.with_structured_output(DiagnosisDraft)` 将研究结论整理成强类型 Pydantic 草案。

### 4. LLM + 规则引擎混合护栏

模型负责「提议」，`check_herb_compatibility`（确定性规则）负责「校验」——每次诊断无论 Agent 是否调用过校验工具，服务端都兜底再跑一次规则引擎，产出可审计的配伍警告。

### 5. 人机协同闭环

`DRAFT → 医生编辑 → 签字(SIGNED) → PDF → 一键转处方`，AI 始终是副驾，最终由医生签字生效。

## 目录结构

```
ai-service/
├── app/
│   ├── agents/       # LLM / 工具集 / prompt / schema / Agent 循环
│   ├── api/          # deps(JWT) + routes(diagnoses / knowledge)
│   ├── core/         # security / response 信封 / errors
│   ├── db/           # engine / schema / read_sql / diagnosis_repo
│   ├── ingest/       # loader / chunker / pipeline（知识库入库）
│   ├── rules/        # 十八反十九畏规则引擎
│   ├── services/     # diagnosis_service / pdf_service
│   ├── vector/       # Milvus client / embedder
│   ├── config.py
│   └── main.py
├── data/classics/    # 古籍样例（伤寒论 / 金匮要略 条文）
├── tests/            # chunker / rules 单元测试
├── Dockerfile
└── requirements.txt
```

## 本地运行

```bash
# 1. 起基础设施（MySQL + Milvus 三容器）
docker compose up -d mysql etcd minio milvus

# 2. 配置密钥
export DEEPSEEK_API_KEY=...
export EMBEDDING_API_KEY=...
export DB_PASSWORD=... JWT_SECRET=...

# 3. 安装依赖并启动
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8085

# 4. 入库古籍
curl -X POST localhost:8085/api/ai/knowledge/ingest -H 'Content-Type: application/json' -d '{"reset":true}'

# 5. 发起诊断（带 JWT）
curl -X POST localhost:8085/api/ai/diagnoses \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"patientId": 1}'
```

## 面试亮点

- **真·工具调用循环**：Agent 自主决定调哪个工具、迭代推理，轨迹可观测（非一次性 RAG）
- **可解释 RAG + 证据溯源**：每条结论锚定 Milvus 检索到的古籍条文，医生可核验出处
- **LLM + 规则引擎混合护栏**：模型提议、`check_herb_compatibility` 确定性校验，可单测可审计
- **领域感知切分**：古文条文感知 chunking 优于通用递归切分
- **结构化输出**：两阶段（研究循环 → with_structured_output）保证强类型 DiagnosisDraft
- **人机协同**：DRAFT→编辑→签字→PDF，AI 是副驾而非自主
- **多语言微服务协作**：Python AI 服务与 Java/Spring Cloud 共存，共享 MySQL + 同一 JWT（PyJWT 验 HS256）
- **一键闭环**：签字后的诊断转正式处方，落回现有工作流
