"""同步 SQLAlchemy engine（pymysql）。

LangChain Agent 执行是同步的（工具调用循环），因此 db 层用同步驱动，
避免 async/sync 桥接。FastAPI 用 def 端点跑在线程池，不会阻塞事件循环。
"""
from sqlalchemy import create_engine
from sqlalchemy.engine import URL
from sqlalchemy.orm import sessionmaker

from app.config import settings

_db_url = URL.create(
    "mysql+pymysql",
    username=settings.db_user,
    password=settings.db_password,
    host=settings.db_host,
    port=settings.db_port,
    database=settings.db_name,
    query={"charset": "utf8mb4"},
)

engine = create_engine(
    _db_url,
    pool_pre_ping=True,
    pool_recycle=3600,
    pool_size=5,
    max_overflow=10,
)

SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)
