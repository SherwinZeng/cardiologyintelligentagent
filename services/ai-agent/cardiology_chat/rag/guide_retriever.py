import logging
from dataclasses import dataclass

from cardiology_chat.rag.guide_names import unique_guide_display_names
from cardiology_chat.rag.guide_rerank import rerank_scored_documents
from cardiology_chat.rag.guide_store import get_guide_vector_store
from cardiology_chat.rag.milvus_uri import is_remote_milvus_uri
from configuration import settings

logger = logging.getLogger(__name__)

RAG_ROUTES = frozenset({"symptom", "lab", "medication", "history"})
EXCERPT_MAX_CHARS = 900


@dataclass(frozen=True)
class GuideRetrievalResult:
    excerpts: list[str]
    guide_references: list[str]


def is_rag_available() -> bool:
    if not settings.RAG_ENABLED:
        return False
    if not settings.ZHIPU_API_KEY:
        return False
    if is_remote_milvus_uri(settings.MILVUS_URI):
        return bool(settings.MILVUS_URI)
    return True


def _format_excerpt(doc) -> str | None:
    content = (doc.page_content or "").strip()
    if not content:
        return None
    guide_name = doc.metadata.get("guide_name") or doc.metadata.get("source") or ""
    if guide_name:
        return f"[{guide_name}] {content[:EXCERPT_MAX_CHARS]}"
    return content[:EXCERPT_MAX_CHARS]


def _collect_guide_stems(docs) -> list[str]:
    stems: list[str] = []
    for doc in docs:
        stem = doc.metadata.get("guide_name") or doc.metadata.get("source") or ""
        if stem:
            stems.append(str(stem))
    return stems


def retrieve_guide_context(query: str, route: str, top_k: int | None = None) -> GuideRetrievalResult:
    return _retrieve_guide_documents(query, route, top_k=top_k)


def retrieve_guide_excerpts(query: str, route: str, top_k: int | None = None) -> list[str]:
    return _retrieve_guide_documents(query, route, top_k=top_k).excerpts


def _retrieve_guide_documents(query: str, route: str, top_k: int | None = None) -> GuideRetrievalResult:
    if route not in RAG_ROUTES or not is_rag_available():
        return GuideRetrievalResult(excerpts=[], guide_references=[])
    text = (query or "").strip()
    if not text:
        return GuideRetrievalResult(excerpts=[], guide_references=[])

    final_k = top_k or settings.RAG_TOP_K
    fetch_k = max(settings.RAG_FETCH_K, final_k)
    try:
        store = get_guide_vector_store()
        candidates = store.similarity_search_with_score(text, k=fetch_k)
        docs = rerank_scored_documents(
            text,
            candidates,
            final_k=final_k,
            vector_weight=settings.RAG_VECTOR_WEIGHT,
            keyword_weight=settings.RAG_KEYWORD_WEIGHT,
        )
        excerpts: list[str] = []
        for doc in docs:
            excerpt = _format_excerpt(doc)
            if excerpt:
                excerpts.append(excerpt)
        return GuideRetrievalResult(
            excerpts=excerpts,
            guide_references=unique_guide_display_names(_collect_guide_stems(docs)),
        )
    except Exception:
        logger.exception("guide RAG retrieve failed | route=%s", route)
        return GuideRetrievalResult(excerpts=[], guide_references=[])
