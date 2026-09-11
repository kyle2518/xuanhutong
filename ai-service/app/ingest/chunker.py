"""古文感知切分：以「条文」为最小单元，超长才按句读切分并带重叠。

中医古籍（《伤寒论》《金匮要略》）天然按编号条文组织，直接逐条文切分
比通用递归字符切分更能保留完整语义（面试亮点：领域感知 chunking）。
"""
import re
from dataclasses import dataclass, asdict

SENTENCE_BOUNDARY = re.compile(r"(?<=[。；！？：])")


@dataclass
class Chunk:
    book_title: str
    chapter: str
    chunk_index: int
    source_text: str

    def to_dict(self) -> dict:
        return asdict(self)


def _split_long(text: str, max_chars: int, overlap: int) -> list[str]:
    """超长条文按句读切分，尽量接近 max_chars，相邻片带 overlap 字符重叠。"""
    sentences = [s for s in SENTENCE_BOUNDARY.split(text) if s]
    pieces: list[str] = []
    buf = ""
    for sent in sentences:
        if len(buf) + len(sent) <= max_chars:
            buf += sent
        else:
            if buf:
                pieces.append(buf)
            # 保留上一片末尾 overlap 字符作为上下文
            buf = (buf[-overlap:] if overlap else "") + sent
            if len(sent) > max_chars:
                # 单句本身超长，硬切
                for i in range(0, len(sent), max_chars):
                    pieces.append(sent[i : i + max_chars])
                buf = ""
    if buf:
        pieces.append(buf)
    return pieces


def chunk_book(book: dict, max_chars: int = 512, overlap: int = 30) -> list[Chunk]:
    chunks: list[Chunk] = []
    idx = 0
    for ch in book.get("chapters", []):
        chapter = ch.get("chapter", "")
        for clause in ch.get("clauses", []):
            text = (clause.get("text") or "").strip()
            if not text:
                continue
            if len(text) <= max_chars:
                chunks.append(Chunk(book["book_title"], chapter, idx, text))
                idx += 1
            else:
                for piece in _split_long(text, max_chars, overlap):
                    chunks.append(Chunk(book["book_title"], chapter, idx, piece))
                    idx += 1
    return chunks
