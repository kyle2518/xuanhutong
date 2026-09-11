"""加载古籍样例数据（JSON）。"""
import json
from pathlib import Path

from app.config import settings


def load_all(dataset: str | None = None) -> list[dict]:
    base = Path(settings.classics_dir)
    manifest_path = base / "manifest.json"

    if manifest_path.exists():
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    else:
        manifest = [{"file": p.name} for p in base.glob("*.json")]

    books: list[dict] = []
    for item in manifest:
        if dataset and item.get("id") != dataset:
            continue
        path = base / item["file"]
        book = json.loads(path.read_text(encoding="utf-8"))
        # 确保有 book_title（manifest 兜底）
        book.setdefault("book_title", item.get("title", path.stem))
        books.append(book)
    return books
