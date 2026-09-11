"""Embedding 封装：阿里云 DashScope text-embedding-v4（OpenAI 兼容端点）。

直接用 httpx 调用，避免 langchain OpenAIEmbeddings 默认携带 base64 encoding 等
额外字段与 DashScope 不兼容。
"""
import httpx

from app.config import settings


class Embedder:
    def __init__(self):
        self.url = settings.embedding_base_url.rstrip("/") + "/embeddings"
        self.api_key = settings.embedding_api_key
        self.model = settings.embedding_model

    def _embed(self, texts: list[str]) -> list[list[float]]:
        resp = httpx.post(
            self.url,
            headers={"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"},
            json={"model": self.model, "input": texts},
            timeout=60.0,
        )
        resp.raise_for_status()
        data = resp.json()
        return [item["embedding"] for item in data["data"]]

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        # DashScope text-embedding-v4 单次 batch 上限 10 条
        out: list[list[float]] = []
        for i in range(0, len(texts), 10):
            out.extend(self._embed(texts[i : i + 10]))
        return out

    def embed_query(self, text: str) -> list[float]:
        return self._embed([text])[0]


_embedder: Embedder | None = None


def get_embedder() -> Embedder:
    global _embedder
    if _embedder is None:
        _embedder = Embedder()
    return _embedder
