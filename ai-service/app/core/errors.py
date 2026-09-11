"""业务异常：与 Java 侧 GlobalExceptionHandler 语义一致（HTTP 200 + {code, message}）。"""


class AppError(Exception):
    def __init__(self, code: int = 5000, message: str = "服务器内部错误"):
        self.code = code
        self.message = message
        super().__init__(message)


# 常用错误码（对齐 Java ErrorCode 习惯，AI 域用 6xxx）
NOT_FOUND = 404
UNAUTHORIZED = 401
AI_AGENT_ERROR = 6001
AI_KNOWLEDGE_EMPTY = 6002
