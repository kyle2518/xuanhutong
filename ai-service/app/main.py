"""FastAPI 应用入口。"""
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.api.routes import diagnoses, knowledge
from app.core.errors import AppError
from app.core.response import fail
from app.db.schema import ensure_tables
from app.vector.client import ensure_collection, get_client

logger = logging.getLogger("ai-service")


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动时幂等建表；MySQL 未就绪不阻断启动
    try:
        ensure_tables()
    except Exception as e:  # noqa: BLE001
        logger.warning("MySQL 未就绪，跳过建表：%s", e)
    # 幂等建集合；Milvus 未就绪不阻断启动（入库走 /ingest）
    try:
        ensure_collection(get_client())
    except Exception as e:  # noqa: BLE001
        logger.warning("Milvus 未就绪，跳过建集合：%s", e)
    yield


app = FastAPI(title="悬壶通 AI 辅助诊断服务", version="0.1.0", lifespan=lifespan)


@app.exception_handler(AppError)
async def app_error_handler(request: Request, exc: AppError):
    # 业务错误：HTTP 200 + {code, message}（与 Java 侧 GlobalExceptionHandler 一致）
    return JSONResponse(status_code=200, content=fail(exc.code, exc.message).model_dump())


app.include_router(knowledge.router, prefix="/api/ai/knowledge", tags=["knowledge"])
app.include_router(diagnoses.router, prefix="/api/ai", tags=["diagnoses"])


@app.get("/healthz")
def healthz():
    return {"status": "ok"}
