"""共享的 Embedding 封装：阿里云 DashScope text-embedding-v4（OpenAI 兼容端点）。

ai-service 与 rag-service 共用，统一处理：
- DashScope text-embedding-v4 单次 batch 上限 10 条；
- 用 httpx 直连（避免 langchain OpenAIEmbeddings 默认携带 base64 encoding 与 DashScope 不兼容）。

本模块不依赖 LangChain，两个服务可各自保持自己的 LangChain 版本。
"""
import httpx


class DashScopeEmbedder:
    BATCH_SIZE = 10  # DashScope text-embedding-v4 单次 batch 上限

    def __init__(self, base_url: str, api_key: str, model: str = "text-embedding-v4"):
        self.url = base_url.rstrip("/") + "/embeddings"
        self.api_key = api_key
        self.model = model

    def _embed(self, texts: list[str]) -> list[list[float]]:
        resp = httpx.post(
            self.url,
            headers={"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"},
            json={"model": self.model, "input": texts},
            timeout=60.0,
        )
        resp.raise_for_status()
        return [item["embedding"] for item in resp.json()["data"]]

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        out: list[list[float]] = []
        for i in range(0, len(texts), self.BATCH_SIZE):
            out.extend(self._embed(texts[i : i + self.BATCH_SIZE]))
        return out

    def embed_query(self, text: str) -> list[float]:
        return self._embed([text])[0]
