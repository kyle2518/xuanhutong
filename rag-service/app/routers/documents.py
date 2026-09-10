import uuid
from pathlib import Path

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile

from ..container import db, settings, store
from ..models import DocumentList, DocumentOut
from ..security import require_auth
from ..services.document_service import (
    SUPPORTED_EXTENSIONS,
    DocumentParseError,
    split_documents,
)

router = APIRouter(prefix="/api/rag", tags=["documents"])


@router.post(
    "/knowledge-bases/{kb_id}/documents",
    response_model=DocumentOut,
    status_code=201,
    dependencies=[Depends(require_auth)],
)
def upload_document(kb_id: str, file: UploadFile = File(...)):
    kb = db.get_knowledge_base(kb_id)
    if not kb:
        raise HTTPException(status_code=404, detail="知识库不存在")
    filename = file.filename or "unnamed"
    ext = Path(filename).suffix.lower()
    if ext not in SUPPORTED_EXTENSIONS:
        raise HTTPException(
            status_code=415,
            detail=f"不支持的文档类型: {ext or '无扩展名'}，支持 {', '.join(sorted(SUPPORTED_EXTENSIONS))}",
        )
    data = file.file.read()
    if len(data) > settings.max_file_size_mb * 1024 * 1024:
        raise HTTPException(
            status_code=413,
            detail=f"文件超过 {settings.max_file_size_mb}MB 限制",
        )
    document_id = uuid.uuid4().hex
    try:
        chunks = split_documents(
            filename,
            data,
            document_id,
            settings.chunk_size,
            settings.chunk_overlap,
        )
    except DocumentParseError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    if not chunks:
        raise HTTPException(status_code=400, detail="文档内容为空，无法建立索引")
    store.add_documents(kb.collection_name, chunks)
    return db.add_document(kb, Path(filename).name, ext, len(chunks))


@router.get(
    "/knowledge-bases/{kb_id}/documents",
    response_model=DocumentList,
    dependencies=[Depends(require_auth)],
)
def list_documents(kb_id: str):
    kb = db.get_knowledge_base(kb_id)
    if not kb:
        raise HTTPException(status_code=404, detail="知识库不存在")
    items = db.list_documents(kb_id)
    return DocumentList(items=items, total=len(items))


@router.delete("/documents/{document_id}", status_code=204, dependencies=[Depends(require_auth)])
def delete_document(document_id: str):
    doc = db.get_document(document_id)
    if not doc:
        raise HTTPException(status_code=404, detail="文档不存在")
    store.delete_documents(doc.collection_name, document_id)
    db.delete_document(document_id)