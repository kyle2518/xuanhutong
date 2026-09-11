"""入库管线：load → chunk → embed(批量) → upsert（幂等，可 reset）。"""
import time

from app.config import settings
from app.ingest.chunker import chunk_book, chunk_text
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


def parse_file(filename: str, content: bytes) -> str:
    """解析上传的典籍文件（txt/md/pdf）为纯文本。"""
    ext = filename.lower().rsplit(".", 1)[-1] if "." in filename else ""
    if ext in ("txt", "md"):
        return content.decode("utf-8", errors="ignore")
    if ext == "pdf":
        import io

        from pypdf import PdfReader

        reader = PdfReader(io.BytesIO(content))
        return "\n\n".join((page.extract_text() or "") for page in reader.pages)
    raise ValueError(f"不支持的文件类型：{ext or '未知'}（仅支持 txt / md / pdf）")


def upload_classic(filename: str, content: bytes, book_title: str | None = None) -> dict:
    """上传一篇典籍：解析 → 切分 → embedding → 入库 tcm_classics。"""
    text = parse_file(filename, content)
    title = book_title or (filename.rsplit(".", 1)[0] if "." in filename else filename)
    chunks = chunk_text(text, title)
    if not chunks:
        raise ValueError("文件内容为空，无法切分")
    client = get_client()
    ensure_collection(client)
    embedder = get_embedder()
    embeddings = embedder.embed_documents([c.source_text for c in chunks])
    n = upsert_chunks(client, [c.to_dict() for c in chunks], embeddings)
    return {"book_title": title, "chunks": n}
