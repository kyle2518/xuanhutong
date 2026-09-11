from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
from langchain_milvus import Milvus
from pymilvus import MilvusClient
from shared.embedding import DashScopeEmbedder

from ..config import Settings


class DashScopeEmbeddings(Embeddings):
    """把共享层的 DashScopeEmbedder 适配为 LangChain Embeddings 接口。"""

    def __init__(self, embedder: DashScopeEmbedder):
        self._embedder = embedder

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return self._embedder.embed_documents(texts)

    def embed_query(self, text: str) -> list[float]:
        return self._embedder.embed_query(text)


class VectorStoreProvider:
    def __init__(self, settings: Settings):
        self._settings = settings
        self._embeddings: Embeddings | None = None

    def _embedding(self) -> Embeddings:
        if self._embeddings is None:
            self._embeddings = DashScopeEmbeddings(
                DashScopeEmbedder(
                    base_url=self._settings.embedding_base_url
                    or self._settings.llm_base_url,
                    api_key=self._settings.embedding_api_key or "",
                    model=self._settings.embedding_model,
                )
            )
        return self._embeddings

    def _connection_args(self) -> dict:
        args = {"uri": self._settings.milvus_uri}
        if self._settings.milvus_token:
            args["token"] = self._settings.milvus_token
        return args

    def _store(self, collection_name: str) -> Milvus:
        return Milvus(
            embedding_function=self._embedding(),
            collection_name=collection_name,
            connection_args=self._connection_args(),
            index_params={"index_type": "AUTOINDEX", "metric_type": "COSINE"},
            enable_dynamic_field=True,
            auto_id=True,
        )

    def add_documents(self, collection_name: str, documents: list[Document]) -> None:
        self._store(collection_name).add_documents(documents)

    def similarity_search(
        self,
        collection_name: str,
        query: str,
        top_k: int,
        expr: str | None = None,
    ) -> list[tuple[Document, float]]:
        return self._store(collection_name).similarity_search_with_score(
            query, k=top_k, expr=expr
        )

    def delete_documents(self, collection_name: str, document_id: str) -> None:
        self._store(collection_name).delete(
            expr=f'metadata["document_id"] == "{document_id}"'
        )

    def has_collection(self, collection_name: str) -> bool:
        return MilvusClient(**self._connection_args()).has_collection(collection_name)

    def drop_collection(self, collection_name: str) -> None:
        try:
            self._store(collection_name).drop()
        except Exception:
            pass

    def ping(self) -> bool:
        try:
            MilvusClient(**self._connection_args()).list_collections()
            return True
        except Exception:
            return False