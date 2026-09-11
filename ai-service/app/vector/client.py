"""Milvus 客户端封装：建集合、写入、检索。

用原生 pymilvus MilvusClient，完全掌控 schema/index（不走 langchain vectorstore 抽象）。
"""
from typing import Any

from pymilvus import DataType, MilvusClient

from app.config import settings

COLLECTION = settings.milvus_collection
DIM = settings.embedding_dim


def get_client() -> MilvusClient:
    return MilvusClient(uri=settings.milvus_uri)


def ensure_collection(client: MilvusClient) -> None:
    """幂等建集合（1024 维 float_vector + HNSW/COSINE 索引）。"""
    if client.has_collection(COLLECTION):
        return

    schema = MilvusClient.create_schema(auto_id=True, enable_dynamic_field=False)
    schema.add_field("id", DataType.INT64, is_primary=True)
    schema.add_field("book_title", DataType.VARCHAR, max_length=256)
    schema.add_field("chapter", DataType.VARCHAR, max_length=256)
    schema.add_field("source_text", DataType.VARCHAR, max_length=8192)
    schema.add_field("chunk_index", DataType.INT64)
    schema.add_field("float_vector", DataType.FLOAT_VECTOR, dim=DIM)

    index_params = MilvusClient.prepare_index_params()
    index_params.add_index(
        field_name="float_vector",
        index_type="HNSW",
        metric_type="COSINE",
        params={"M": 16, "efConstruction": 200},
    )

    client.create_collection(COLLECTION, schema=schema, index_params=index_params)


def reset_collection(client: MilvusClient) -> None:
    if client.has_collection(COLLECTION):
        client.drop_collection(COLLECTION)
    ensure_collection(client)


def upsert_chunks(
    client: MilvusClient,
    chunks: list[dict[str, Any]],
    embeddings: list[list[float]],
) -> int:
    """chunks 与 embeddings 一一对应；返回写入行数。"""
    rows = [
        {
            "book_title": c["book_title"],
            "chapter": c["chapter"],
            "source_text": c["source_text"],
            "chunk_index": c["chunk_index"],
            "float_vector": emb,
        }
        for c, emb in zip(chunks, embeddings)
    ]
    if not rows:
        return 0
    return client.insert(collection_name=COLLECTION, data=rows)["insert_count"]


def search(
    client: MilvusClient,
    query_embedding: list[float],
    top_k: int = 5,
) -> list[dict[str, Any]]:
    """检索 top_k 相关古籍片段，返回含标量字段的命中列表。"""
    res = client.search(
        collection_name=COLLECTION,
        data=[query_embedding],
        limit=top_k,
        output_fields=["book_title", "chapter", "source_text", "chunk_index"],
        search_params={"metric_type": "COSINE", "params": {"ef": 64}},
    )
    hits = res[0] if res else []
    return [
        {
            "book_title": h["entity"].get("book_title"),
            "chapter": h["entity"].get("chapter"),
            "source_text": h["entity"].get("source_text"),
            "chunk_index": h["entity"].get("chunk_index"),
            "score": h.get("distance"),
        }
        for h in hits
    ]


def count(client: MilvusClient) -> int:
    client.flush(COLLECTION)
    stats = client.get_collection_stats(COLLECTION)
    return int(stats.get("row_count", 0))


def list_book_titles(client: MilvusClient) -> list[dict]:
    """列出 tcm_classics 中各典籍（distinct book_title + chunk 数）。"""
    import ast

    client.flush(COLLECTION)
    res = client.query(
        collection_name=COLLECTION,
        filter="id >= 0",
        output_fields=["book_title"],
        limit=10000,
    )
    data = getattr(res, "data", res) if not isinstance(res, list) else res
    titles: dict[str, int] = {}
    for item in data:
        bt = None
        if isinstance(item, dict):
            bt = item.get("book_title")
        elif isinstance(item, str):
            try:
                bt = ast.literal_eval(item).get("book_title")
            except Exception:
                continue
        if bt:
            titles[bt] = titles.get(bt, 0) + 1
    return [{"book_title": k, "chunks": v} for k, v in titles.items()]
