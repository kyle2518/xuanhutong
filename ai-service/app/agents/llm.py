"""DeepSeek LLM 工厂（OpenAI 兼容端点）。"""
from functools import lru_cache

from langchain_openai import ChatOpenAI

from app.config import settings


@lru_cache(maxsize=1)
def get_llm(temperature: float = 0) -> ChatOpenAI:
    return ChatOpenAI(
        model=settings.deepseek_model,
        base_url=settings.deepseek_base_url,
        api_key=settings.deepseek_api_key,
        temperature=temperature,
    )
