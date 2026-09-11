"""FastAPI 依赖：解析 JWT 得到当前用户 id。"""
from fastapi import HTTPException, Request

from app.core.security import user_id_from_token


def get_current_user_id(request: Request) -> int:
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="未登录")
    token = auth.removeprefix("Bearer ")
    try:
        return user_id_from_token(token)
    except Exception:
        raise HTTPException(status_code=401, detail="token 无效")
