import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from .container import db, settings, store
from .models import HealthOut
from .routers import documents, knowledge_bases, query

logger = logging.getLogger("rag-service")


@asynccontextmanager
async def lifespan(_: FastAPI):
    db.init_db()
    yield


app = FastAPI(
    title="悬壶通 RAG 知识库服务",
    description="基于 LangChain + Milvus 的文档知识库与检索问答服务",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(Exception)
async def unhandled_exception_handler(_: Request, exc: Exception):
    logger.exception("Unhandled error: %s", exc)
    return JSONResponse(status_code=500, content={"detail": "Internal Server Error"})

app.include_router(knowledge_bases.router)
app.include_router(documents.router)
app.include_router(query.router)


@app.get("/api/rag/health", response_model=HealthOut, tags=["health"])
def health():
    return HealthOut(
        status="ok",
        milvus="up" if store.ping() else "down",
        llm_configured=bool(settings.llm_api_key),
        embedding_configured=bool(settings.embedding_api_key),
    )