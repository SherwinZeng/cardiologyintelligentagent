from langchain_community.embeddings import ZhipuAIEmbeddings

from configuration import settings

# 智谱 embeddings API：单次 input 最多 64 条
ZHIPU_EMBED_BATCH_SIZE = 32

_embeddings: ZhipuAIEmbeddings | None = None


class _BatchingZhipuEmbeddings(ZhipuAIEmbeddings):
    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        vectors: list[list[float]] = []
        batch_size = ZHIPU_EMBED_BATCH_SIZE
        for start in range(0, len(texts), batch_size):
            batch = texts[start : start + batch_size]
            vectors.extend(super().embed_documents(batch))
        return vectors


def get_zhipu_embeddings() -> ZhipuAIEmbeddings:
    global _embeddings
    if _embeddings is None:
        if not settings.ZHIPU_API_KEY:
            raise ValueError("请在 .env 中配置 ZHIPU_API_KEY")
        _embeddings = _BatchingZhipuEmbeddings(
            api_key=settings.ZHIPU_API_KEY,
            model=settings.ZHIPU_EMBEDDING_MODEL,
        )
    return _embeddings
