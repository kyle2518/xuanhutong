from fastapi import APIRouter, Depends, HTTPException

from ..container import db, rag
from ..models import QueryRequest, QueryResponse, SearchRequest, SearchResponse
from ..security import require_auth

router = APIRouter(prefix="/api/rag", tags=["query"], dependencies=[Depends(require_auth)])


def _resolve_knowledge_bases(knowledge_base_id: str | None):
    if knowledge_base_id:
        kb = db.get_knowledge_base(knowledge_base_id)
        if not kb:
            raise HTTPException(status_code=404, detail="知识库不存在")
        return [kb]
    return db.list_knowledge_bases()


@router.post("/search", response_model=SearchResponse)
def search_documents(payload: SearchRequest):
    knowledge_bases = _resolve_knowledge_bases(payload.knowledge_base_id)
    if not knowledge_bases:
        raise HTTPException(status_code=404, detail="尚未创建任何知识库")
    hits = rag.search(payload.query, knowledge_bases, top_k=payload.top_k)
    return SearchResponse(query=payload.query, hits=hits, count=len(hits))


@router.post("/query", response_model=QueryResponse)
def query_knowledge(payload: QueryRequest):
    knowledge_bases = _resolve_knowledge_bases(payload.knowledge_base_id)
    if not knowledge_bases:
        raise HTTPException(status_code=404, detail="尚未创建任何知识库")
    hits = rag.search(payload.question, knowledge_bases, top_k=payload.top_k)
    if not hits:
        raise HTTPException(status_code=404, detail="知识库中没有检索到相关内容")
    answer = rag.answer(payload.question, hits, payload.system_prompt)
    return QueryResponse(question=payload.question, answer=answer, sources=hits)