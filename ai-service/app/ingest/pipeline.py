"""入库管线：load → chunk → embed(批量) → upsert（幂等，可 reset）。"""
import time

from app.config import settings
from app.ingest.chunker import chunk_book
from app.ingest.loader import load_all
from app.vector.client import (
    count,
    ensure_collection,
    get_client,
    reset_collection,
    upsert_chunks,
)
from app.vector.embedder import get_embedder


def run_ingest(reset: bool = False, dataset: str | None = None) -> dict:
    start = time.perf_counter()
    client = get_client()
    if reset:
        reset_collection(client)
    else:
        ensure_collection(client)

    embedder = get_embedder()
    total = 0
    for book in load_all(dataset):
        chunks = chunk_book(book)
        if not chunks:
            continue
        texts = [c.source_text for c in chunks]
        embeddings = embedder.embed_documents(texts)
        total += upsert_chunks(client, [c.to_dict() for c in chunks], embeddings)

    return {
        "chunks": total,
        "collection": settings.milvus_collection,
        "total_in_collection": count(client),
        "elapsed_s": round(time.perf_counter() - start, 2),
    }
