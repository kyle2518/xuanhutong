import os

import jwt
from fastapi import Header, HTTPException

_JWT_ALGORITHMS = ["HS256", "HS384", "HS512"]


def _secret() -> str:
    secret = os.environ.get("JWT_SECRET", "")
    if not secret:
        raise HTTPException(status_code=503, detail="服务端未配置 JWT_SECRET")
    return secret


def require_auth(authorization: str | None = Header(default=None)) -> dict:
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="未提供有效的认证令牌")
    token = authorization.split(" ", 1)[1].strip()
    try:
        return jwt.decode(token, _secret(), algorithms=_JWT_ALGORITHMS)
    except jwt.PyJWTError as exc:
        raise HTTPException(status_code=401, detail="认证令牌无效或已过期") from exc