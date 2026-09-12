# 悬壶通 RAG 知识库服务

基于 **LangChain**（Python）+ **Milvus** 的文档知识库与检索问答（RAG）微服务，
供悬壶通中医诊所管理系统提供“基于自有知识库的智能问答”能力。

## 技术栈

| 组件 | 说明 |
| ---- | ---- |
| [LangChain](https://python.langchain.com) | LLM 应用编排框架（LCEL） |
| [FastAPI](https://fastapi.tiangolo.com) | 异步 Web 框架，对外提供 REST API |
| [Milvus](https://milvus.io) | 向量数据库（独立部署，v2.6） |
| [langchain-milvus](https://github.com/langchain-ai/langchain-milvus) | LangChain 官方 Milvus 向量存储集成 |
| OpenAI 兼容接口 | 对接 OpenAI / DeepSeek / 通义千问 / Kimi / 智谱 等任意兼容模型的 Chat 与 Embedding |

> 选择 OpenAI 兼容接口（`base_url` 可配置），可平滑切换国内外任意大模型，
> Chat 模型与 Embedding 模型可分别指定。

## 架构

```
前端 / 其他微服务
      │  /api/rag/**
      ▼
  Spring Cloud Gateway (:8080)
      │
      ▼
  rag-service (FastAPI :8000)
      │  LangChain RAG 链路
      ├── 向量化/检索 ────►  Milvus (:19530) ──► etcd + MinIO（依赖组件）
      └── 生成回答 ────────►  LLM（OpenAI 兼容接口）
```

- 每个知识库对应 Milvus 中的一个 Collection；
- 知识库与文档的元数据存于 SQLite（`data/rag_metadata.db`，容器内挂载 volume 持久化）；
- 文档支持 `.txt / .md / .pdf / .docx`，先分块（RecursiveCharacterTextSplitter）再向量化入库；
- 除 `/health` 外所有接口要求 **Bearer JWT 认证**（与各 Java 微服务共用 `JWT_SECRET`，请求头 `Authorization: Bearer <token>`，由前端 Axios 拦截器自动携带）。

## 本地开发

```bash
cd rag-service
python -m venv .venv
.venv\Scripts\activate                # Windows（Linux/macOS 用 source .venv/bin/activate）
pip install -r requirements.txt
cp .env.example .env                 # 填写 RAG_LLM_API_KEY 等
uvicorn app.main:app --reload --port 8000
```

> 需要 Python 3.10+（推荐 3.12，与 Docker 镜像一致）。

本地需先启动 Milvus（也可使用 `docker compose up -d etcd minio milvus`）。

## Docker / docker-compose

在项目根目录执行（需先配置根目录 `.env`，模板见 `.env.example`）：

```bash
docker compose up -d rag-service
```

Milvus 与其依赖（etcd、MinIO）同一 compose 中自动编排。

## REST API

所有接口挂在 `/api/rag/**` 前缀下，经网关转发。

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/rag/health` | 健康检查（含 Milvus/LLM 状态） |
| POST | `/api/rag/knowledge-bases` | 创建知识库 `{name, description?}` |
| GET | `/api/rag/knowledge-bases` | 知识库列表 |
| GET | `/api/rag/knowledge-bases/{kb_id}` | 知识库详情 |
| DELETE | `/api/rag/knowledge-bases/{kb_id}` | 删除知识库（连向量数据） |
| POST | `/api/rag/knowledge-bases/{kb_id}/documents` | 上传文档（multipart，支持 txt/md/pdf/docx） |
| GET | `/api/rag/knowledge-bases/{kb_id}/documents` | 知识库文档列表 |
| DELETE | `/api/rag/documents/{document_id}` | 删除文档（连向量数据） |
| POST | `/api/rag/search` | 语义检索 `{query, knowledge_base_id?, top_k?}`，返回命中片段 |
| POST | `/api/rag/query` | RAG 问答 `{question, knowledge_base_id?, top_k?, system_prompt?}` |

调用示例：

```bash
# 登录获取 JWT（见后端认证接口），例如：
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800000000","code":"1234"}' | jq -r '.data.token')

# 创建知识库
curl -X POST http://localhost:8080/api/rag/knowledge-bases \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"中医方剂库","description":"经典方剂资料"}'

# 上传文档
curl -X POST http://localhost:8080/api/rag/knowledge-bases/{kb_id}/documents \
  -H "Authorization: Bearer $TOKEN" \
  -F 'file=@方剂.md'

# 基于知识库问答
curl -X POST http://localhost:8080/api/rag/query \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"question":"四君子汤的组成和功效是什么？","knowledge_base_id":"{kb_id}"}'
```

## 环境变量

| 变量 | 默认值 | 说明 |
| ---- | ---- | ---- |
| `JWT_SECRET` | 空 | 与后端共享的 JWT 签名密钥（认证必需） |
| `RAG_MILVUS_URI` | `http://localhost:19530` | Milvus 地址 |
| `RAG_MILVUS_TOKEN` | 空 | Milvus 认证 token（如 `root:Milvus`） |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com/v1` | Chat 模型 OpenAI 兼容地址（与 ai-service 共用） |
| `DEEPSEEK_API_KEY` | 空（回退 `OPENAI_API_KEY`） | Chat 模型 API Key（与 ai-service 共用） |
| `DEEPSEEK_MODEL` | `deepseek-v4-pro` | Chat 模型名（与 ai-service 共用） |
| `RAG_LLM_TEMPERATURE` | `0.2` | 生成温度（rag 特有） |
| `EMBEDDING_MODEL` | `text-embedding-v4` | Embedding 模型名（与 ai-service 共用） |
| `EMBEDDING_BASE_URL` / `EMBEDDING_API_KEY` | 复用 LLM | Embedding 接口（与 ai-service 共用） |
| `RAG_CHUNK_SIZE` / `RAG_CHUNK_OVERLAP` | `500` / `50` | 文本分块参数 |
| `RAG_DEFAULT_TOP_K` | `5` | 默认检索条数 |
| `RAG_MAX_FILE_SIZE_MB` | `20` | 单文档大小上限 |
| `RAG_METADATA_DB_PATH` | `data/rag_metadata.db` | 元数据 SQLite 路径 |

## 备注

- 更换 Embedding 模型后，**已有 Collection 的向量维度不兼容，需要重建知识库**；
- Milvus 数据（向量）+ MinIO + etcd + rag 元数据均通过 volume 持久化，删除容器不丢数据。