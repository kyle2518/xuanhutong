import uuid

from fastapi import APIRouter, Depends, HTTPException

from ..container import db, store
from ..models import KnowledgeBaseCreate, KnowledgeBaseList, KnowledgeBaseOut
from ..security import require_auth

router = APIRouter(prefix="/api/rag/knowledge-bases", tags=["knowledge-bases"])


@router.post("", response_model=KnowledgeBaseOut, status_code=201, dependencies=[Depends(require_auth)])
def create_knowledge_base(payload: KnowledgeBaseCreate):
    existing = [kb for kb in db.list_knowledge_bases() if kb.name == payload.name]
    if existing:
        raise HTTPException(status_code=409, detail=f"知识库「{payload.name}」已存在")
    collection_name = f"kb_{uuid.uuid4().hex}"
    return db.create_knowledge_base(
        payload.name, payload.description, collection_name
    )


@router.get("", response_model=KnowledgeBaseList, dependencies=[Depends(require_auth)])
def list_knowledge_bases():
    items = db.list_knowledge_bases()
    return KnowledgeBaseList(items=items, total=len(items))


@router.get("/{kb_id}", response_model=KnowledgeBaseOut, dependencies=[Depends(require_auth)])
def get_knowledge_base(kb_id: str):
    kb = db.get_knowledge_base(kb_id)
    if not kb:
        raise HTTPException(status_code=404, detail="知识库不存在")
    return kb


@router.delete("/{kb_id}", status_code=204, dependencies=[Depends(require_auth)])
def delete_knowledge_base(kb_id: str):
    kb = db.get_knowledge_base(kb_id)
    if not kb:
        raise HTTPException(status_code=404, detail="知识库不存在")
    store.drop_collection(kb.collection_name)
    db.delete_knowledge_base(kb_id)