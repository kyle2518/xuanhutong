from pydantic import BaseModel, Field


class KnowledgeBaseCreate(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    description: str = ""


class KnowledgeBaseOut(BaseModel):
    id: str
    name: str
    description: str
    collection_name: str
    document_count: int = 0
    chunk_count: int = 0
    created_at: str
    updated_at: str


class KnowledgeBaseList(BaseModel):
    items: list[KnowledgeBaseOut]
    total: int


class DocumentOut(BaseModel):
    id: str
    knowledge_base_id: str
    collection_name: str
    filename: str
    file_type: str
    chunk_count: int
    status: str
    error: str | None = None
    created_at: str


class DocumentList(BaseModel):
    items: list[DocumentOut]
    total: int


class SearchHit(BaseModel):
    document_id: str
    source: str
    chunk: str
    score: float
    knowledge_base_id: str


class SearchResponse(BaseModel):
    query: str
    hits: list[SearchHit]
    count: int


class QueryRequest(BaseModel):
    question: str = Field(min_length=1)
    knowledge_base_id: str | None = None
    top_k: int | None = Field(default=None, ge=1, le=20)
    system_prompt: str | None = None


class SearchRequest(BaseModel):
    query: str = Field(min_length=1)
    knowledge_base_id: str | None = None
    top_k: int | None = Field(default=None, ge=1, le=20)


class QueryResponse(BaseModel):
    question: str
    answer: str
    sources: list[SearchHit]


class HealthOut(BaseModel):
    status: str
    milvus: str
    llm_configured: bool
    embedding_configured: bool