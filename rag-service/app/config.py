import os
from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


def _llm_api_key_default() -> str:
    return os.environ.get("DEEPSEEK_API_KEY", "") or os.environ.get("OPENAI_API_KEY", "")


def _embedding_api_key_default() -> str:
    return os.environ.get("EMBEDDING_API_KEY", "") or os.environ.get("OPENAI_API_KEY", "")


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_prefix="RAG_", extra="ignore")

    app_name: str = "xuanhutong-rag"
    metadata_db_path: str = "data/rag_metadata.db"
    max_file_size_mb: int = 20
    default_top_k: int = 5
    chunk_size: int = 500
    chunk_overlap: int = 50

    milvus_uri: str = "http://localhost:19530"
    milvus_token: str = ""

    # LLM 与 ai-service 统一走 DEEPSEEK_*（key/base_url/model 共用）
    llm_base_url: str = Field(default="https://api.deepseek.com/v1", validation_alias="DEEPSEEK_BASE_URL")
    llm_model: str = Field(default="deepseek-v4-pro", validation_alias="DEEPSEEK_MODEL")
    llm_temperature: float = 0.2
    llm_timeout: float = 60.0
    llm_api_key: str = Field(default_factory=_llm_api_key_default, validation_alias="DEEPSEEK_API_KEY")

    # embedding 与 ai-service 统一走 EMBEDDING_*（不带 RAG_ 前缀），共享同一个 DashScope 配置
    embedding_base_url: str | None = Field(default=None, validation_alias="EMBEDDING_BASE_URL")
    embedding_model: str = Field(default="text-embedding-v4", validation_alias="EMBEDDING_MODEL")
    embedding_api_key: str = Field(default_factory=_embedding_api_key_default, validation_alias="EMBEDDING_API_KEY")


@lru_cache
def get_settings() -> Settings:
    return Settings()