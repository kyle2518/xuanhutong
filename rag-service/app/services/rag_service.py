from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from ..config import Settings
from ..models import KnowledgeBaseOut, SearchHit
from .vector_store import VectorStoreProvider

DEFAULT_SYSTEM_PROMPT = (
    "你是一名中医诊所的知识库问答助手，精通中医基础理论、方剂、中药、诊断与治疗。"
    "回答用户问题时，请严格遵循以下规则："
    "1. 如果参考资料中包含答案，请结合资料准确作答，可适当补充说明，并在回答中引用资料内容；"
    "2. 如果参考资料中没有相关内容，你可以结合自身中医知识作答，"
    "   但必须在回答的最开头，用醒目的一行单独标注："
    "   【提示：知识库中未找到相关资料，以下内容来自模型知识，仅供参考，不构成诊疗建议，请遵医嘱】；"
    "3. 回答使用简体中文，条理清晰；医学内容需严谨，涉及剂量、配伍禁忌时务必谨慎提示。"
)


def _similarity(score: float) -> float:
    return round(1.0 - float(score), 4)


class RagService:
    def __init__(
        self,
        settings: Settings,
        store: VectorStoreProvider,
    ):
        self._settings = settings
        self._store = store
        self._chain = None

    def _get_chain(self):
        if self._chain is None:
            llm = ChatOpenAI(
                model=self._settings.llm_model,
                base_url=self._settings.llm_base_url,
                api_key=self._settings.llm_api_key or None,
                temperature=self._settings.llm_temperature,
                timeout=self._settings.llm_timeout,
            )
            prompt = ChatPromptTemplate.from_messages(
                [
                    ("system", "{system_prompt}"),
                    ("human", "以下是相关参考资料：\n\n{context}\n\n用户问题：{question}"),
                ]
            )
            self._chain = prompt | llm | StrOutputParser()
        return self._chain

    def search(
        self,
        query: str,
        knowledge_bases: list[KnowledgeBaseOut],
        top_k: int | None = None,
        document_id: str | None = None,
    ) -> list[SearchHit]:
        top_k = top_k or self._settings.default_top_k
        expr = (
            f'metadata["document_id"] == "{document_id}"' if document_id else None
        )
        hits: list[SearchHit] = []
        for kb in knowledge_bases:
            if not self._store.has_collection(kb.collection_name):
                continue
            results = self._store.similarity_search(
                kb.collection_name, query, top_k=top_k, expr=expr
            )
            for doc, distance in results:
                hits.append(
                    SearchHit(
                        document_id=doc.metadata.get("document_id", ""),
                        source=doc.metadata.get("source", ""),
                        chunk=doc.page_content,
                        score=_similarity(distance),
                        knowledge_base_id=kb.id,
                    )
                )
        hits.sort(key=lambda hit: hit.score, reverse=True)
        return hits[:top_k]

    def answer(
        self,
        question: str,
        hits: list[SearchHit],
        system_prompt: str | None = None,
    ) -> str:
        context = "\n\n".join(hit.chunk for hit in hits)
        prompt = system_prompt or DEFAULT_SYSTEM_PROMPT
        return self._get_chain().invoke(
            {"system_prompt": prompt, "context": context, "question": question}
        )