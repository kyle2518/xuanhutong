"""Embedding 封装：走共享层的 DashScope text-embedding-v4。"""
from functools import lru_cache

from shared.embedding import DashScopeEmbedder

from app.config import settings


@lru_cache(maxsize=1)
def get_embedder() -> DashScopeEmbedder:
    return DashScopeEmbedder(
        base_url=settings.embedding_base_url,
        api_key=settings.embedding_api_key,
        model=settings.embedding_model,
    )
