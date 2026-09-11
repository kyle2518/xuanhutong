from app.ingest.chunker import chunk_book


def _book(clauses):
    return {"book_title": "伤寒论", "chapters": [{"chapter": "辨太阳病脉证并治上", "clauses": clauses}]}


def test_short_clause_is_single_chunk():
    chunks = chunk_book(_book([{"no": 1, "text": "太阳之为病，脉浮，头项强痛而恶寒。"}]))
    assert len(chunks) == 1
    assert chunks[0].book_title == "伤寒论"
    assert chunks[0].chapter == "辨太阳病脉证并治上"
    assert chunks[0].source_text == "太阳之为病，脉浮，头项强痛而恶寒。"


def test_long_clause_is_split():
    long_text = "风寒束表，恶寒发热。" * 80  # 远超 512 字
    chunks = chunk_book(_book([{"no": 2, "text": long_text}]))
    assert len(chunks) > 1
    assert all(c.source_text for c in chunks)
    # 拼接去重后应覆盖原文（重叠部分可能重复，这里只验证非空且有序）
    assert all(len(c.source_text) <= 512 + 40 for c in chunks)


def test_chunk_index_monotonic():
    clauses = [
        {"no": 1, "text": "第一条。"},
        {"no": 2, "text": "第二条。"},
        {"no": 3, "text": "第三条。"},
    ]
    chunks = chunk_book(_book(clauses))
    assert [c.chunk_index for c in chunks] == [0, 1, 2]


def test_empty_clause_skipped():
    chunks = chunk_book(_book([{"no": 1, "text": "   "}, {"no": 2, "text": "有效条文。"}]))
    assert len(chunks) == 1
    assert chunks[0].source_text == "有效条文。"
