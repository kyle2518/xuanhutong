import os
import sqlite3
import threading
import uuid
from datetime import datetime, timezone

from .models import DocumentOut, KnowledgeBaseOut


def _utcnow() -> str:
    return datetime.now(timezone.utc).isoformat()


class MetadataDatabase:
    def __init__(self, path: str):
        self._path = path
        self._local = threading.local()

    def _conn(self) -> sqlite3.Connection:
        conn = getattr(self._local, "conn", None)
        if conn is None:
            parent = os.path.dirname(self._path)
            if parent:
                os.makedirs(parent, exist_ok=True)
            conn = sqlite3.connect(self._path)
            conn.row_factory = sqlite3.Row
            self._local.conn = conn
        return conn

    def init_db(self) -> None:
        conn = self._conn()
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS knowledge_bases (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                collection_name TEXT NOT NULL UNIQUE,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS documents (
                id TEXT PRIMARY KEY,
                knowledge_base_id TEXT NOT NULL,
                collection_name TEXT NOT NULL,
                filename TEXT NOT NULL,
                file_type TEXT NOT NULL,
                chunk_count INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'ready',
                error TEXT,
                created_at TEXT NOT NULL
            )
            """
        )
        conn.commit()

    def create_knowledge_base(
        self, name: str, description: str, collection_name: str
    ) -> KnowledgeBaseOut:
        kb_id = uuid.uuid4().hex
        now = _utcnow()
        self._conn().execute(
            "INSERT INTO knowledge_bases (id, name, description, collection_name, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            (kb_id, name, description, collection_name, now, now),
        )
        self._conn().commit()
        return self.get_knowledge_base(kb_id)

    def get_knowledge_base(self, kb_id: str) -> KnowledgeBaseOut | None:
        row = self._conn().execute(
            "SELECT * FROM knowledge_bases WHERE id = ?", (kb_id,)
        ).fetchone()
        return self._kb_from_row(row) if row else None

    def list_knowledge_bases(self) -> list[KnowledgeBaseOut]:
        rows = self._conn().execute(
            "SELECT * FROM knowledge_bases ORDER BY created_at DESC"
        ).fetchall()
        return [self._kb_from_row(row) for row in rows]

    def delete_knowledge_base(self, kb_id: str) -> None:
        self._conn().execute("DELETE FROM documents WHERE knowledge_base_id = ?", (kb_id,))
        self._conn().execute("DELETE FROM knowledge_bases WHERE id = ?", (kb_id,))
        self._conn().commit()

    def add_document(
        self,
        kb: KnowledgeBaseOut,
        filename: str,
        file_type: str,
        chunk_count: int,
    ) -> DocumentOut:
        doc_id = uuid.uuid4().hex
        now = _utcnow()
        self._conn().execute(
            "INSERT INTO documents (id, knowledge_base_id, collection_name, filename, file_type, chunk_count, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'ready', ?)",
            (doc_id, kb.id, kb.collection_name, filename, file_type, chunk_count, now),
        )
        self._conn().execute(
            "UPDATE knowledge_bases SET updated_at = ? WHERE id = ?", (now, kb.id)
        )
        self._conn().commit()
        return self.get_document(doc_id)

    def get_document(self, doc_id: str) -> DocumentOut | None:
        row = self._conn().execute(
            "SELECT * FROM documents WHERE id = ?", (doc_id,)
        ).fetchone()
        if not row:
            return None
        return DocumentOut(
            id=row["id"],
            knowledge_base_id=row["knowledge_base_id"],
            collection_name=row["collection_name"],
            filename=row["filename"],
            file_type=row["file_type"],
            chunk_count=row["chunk_count"],
            status=row["status"],
            error=row["error"],
            created_at=row["created_at"],
        )

    def list_documents(self, kb_id: str) -> list[DocumentOut]:
        rows = self._conn().execute(
            "SELECT * FROM documents WHERE knowledge_base_id = ? ORDER BY created_at DESC",
            (kb_id,),
        ).fetchall()
        return [
            DocumentOut(
                id=row["id"],
                knowledge_base_id=row["knowledge_base_id"],
                collection_name=row["collection_name"],
                filename=row["filename"],
                file_type=row["file_type"],
                chunk_count=row["chunk_count"],
                status=row["status"],
                error=row["error"],
                created_at=row["created_at"],
            )
            for row in rows
        ]

    def delete_document(self, doc_id: str) -> DocumentOut | None:
        doc = self.get_document(doc_id)
        if doc is None:
            return None
        self._conn().execute("DELETE FROM documents WHERE id = ?", (doc_id,))
        self._conn().commit()
        return doc

    def _kb_from_row(self, row: sqlite3.Row) -> KnowledgeBaseOut:
        doc_count = self._conn().execute(
            "SELECT COUNT(*) FROM documents WHERE knowledge_base_id = ?", (row["id"],)
        ).fetchone()[0]
        chunk_count = self._conn().execute(
            "SELECT COALESCE(SUM(chunk_count), 0) FROM documents WHERE knowledge_base_id = ?",
            (row["id"],),
        ).fetchone()[0]
        return KnowledgeBaseOut(
            id=row["id"],
            name=row["name"],
            description=row["description"],
            collection_name=row["collection_name"],
            document_count=doc_count,
            chunk_count=chunk_count,
            created_at=row["created_at"],
            updated_at=row["updated_at"],
        )