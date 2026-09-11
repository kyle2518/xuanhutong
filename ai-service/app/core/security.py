"""JWT 鉴权：复用 Java 侧签发的 HS256 token（sub=userId）。"""
import jwt

from app.config import settings


def decode_token(token: str) -> dict:
    # Java jjwt 依据 secret 长度自动选算法（58 字节 → HS384），兼容整个 HS 家族
    return jwt.decode(token, settings.jwt_secret, algorithms=["HS256", "HS384", "HS512"])


def user_id_from_token(token: str) -> int:
    return int(decode_token(token)["sub"])
