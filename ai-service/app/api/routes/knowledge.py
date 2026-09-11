"""知识库（典籍）管理接口：入库 / 状态 / 上传典籍 / 典籍列表。"""
from fastapi import APIRouter, Depends, File, Form, UploadFile
from pydantic import BaseModel

from app.api.deps import get_current_user_id
from app.core.errors import AppError
from app.core.response import ok
from app.ingest.pipeline import run_ingest, upload_classic
from app.vector.client import count, get_client, list_book_titles

router = APIRouter()


class IngestRequest(BaseModel):
    dataset: str | None = None
    reset: bool = False


@router.post("/ingest")
def ingest(req: IngestRequest):
    """入库 / 重建样例古籍（同步执行，返回入库条数）。"""
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


@router.post("/upload")
async def upload_classic_file(
    file: UploadFile = File(...),
    bookTitle: str | None = Form(None),
    user_id: int = Depends(get_current_user_id),
):
    """上传一篇中医典籍（txt/md/pdf），解析后入库向量库。"""
    content = await file.read()
    try:
        result = upload_classic(file.filename or "untitled", content, bookTitle)
    except ValueError as e:
        raise AppError(400, str(e)) from e
    return ok(result)


@router.get("/classics")
def list_classics(user_id: int = Depends(get_current_user_id)):
    """列出已入库的中医典籍（distinct book_title + chunk 数）。"""
    return ok(list_book_titles(get_client()))
