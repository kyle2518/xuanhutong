"""知识库管理接口：入库 / 状态。"""
from fastapi import APIRouter
from pydantic import BaseModel

from app.core.response import ok
from app.ingest.pipeline import run_ingest
from app.vector.client import count, get_client

router = APIRouter()


class IngestRequest(BaseModel):
    dataset: str | None = None
    reset: bool = False


@router.post("/ingest")
def ingest(req: IngestRequest):
    """入库 / 重建知识库（同步执行，返回入库条数）。"""
    result = run_ingest(reset=req.reset, dataset=req.dataset)
    return ok(result)


@router.get("/status")
def status():
    """知识库健康状态。"""
    client = get_client()
    return ok(
        {
            "collection": client.has_collection("tcm_classics") and "tcm_classics",
            "count": count(client),
        }
    )
