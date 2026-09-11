"""统一响应信封，与 Java 侧 {code, message, data} 保持一致。"""
from typing import Any

from pydantic import BaseModel


class ApiResponse(BaseModel):
    code: int = 200
    message: str = "success"
    data: Any = None


def ok(data: Any = None, message: str = "success") -> ApiResponse:
    return ApiResponse(code=200, message=message, data=data)


def fail(code: int, message: str) -> ApiResponse:
    return ApiResponse(code=code, message=message, data=None)
