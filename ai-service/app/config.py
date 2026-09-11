"""集中配置，全部由环境变量驱动（生产走 docker-compose 注入）。"""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # 应用
    app_port: int = 8085

    # 数据库（只读患者/病历/药材/经典方，读写 ai_diagnoses/prescriptions）
    # 用同步 pymysql 驱动：LangChain Agent 执行本质是同步的，同步 db 避免 async/sync 桥接复杂度
    # 分字段 + URL.create 编码，避免密码特殊字符破坏连接串
    db_host: str = "localhost"
    db_port: int = 3306
    db_user: str = "root"
    db_password: str = "password"
    db_name: str = "xuanhutong"

    # JWT（与 Java 侧同一 secret）
    jwt_secret: str = "xuanhutong-jwt-secret-key-2024-please-change-in-production"

    # DeepSeek（OpenAI 兼容）
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com/v1"
    deepseek_model: str = "deepseek-v4-pro"

    # Embedding（阿里云 DashScope text-embedding-v4，OpenAI 兼容）
    embedding_api_key: str = ""
    embedding_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    embedding_model: str = "text-embedding-v4"
    embedding_dim: int = 1024

    # Milvus
    milvus_uri: str = "http://localhost:19530"
    milvus_collection: str = "tcm_classics"

    # rag-service（知识库检索，用于结合现代药理规范）
    rag_base_url: str = "http://localhost:8000"

    # 古籍样例目录（相对 ai-service 根）
    classics_dir: str = "data/classics"


settings = Settings()
