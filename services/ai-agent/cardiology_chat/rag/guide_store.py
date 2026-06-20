from langchain_milvus import Milvus
from pymilvus import MilvusClient, connections

from cardiology_chat.rag.embeddings import get_zhipu_embeddings
from cardiology_chat.rag.milvus_uri import is_remote_milvus_uri, resolve_milvus_uri
from configuration import settings

_store: Milvus | None = None
_milvus_probe: MilvusClient | None = None


def reset_guide_vector_store() -> None:
    global _store, _milvus_probe
    _store = None
    _milvus_probe = None


def _prepare_milvus_orm_connection(connection_args: dict[str, str]) -> None:
    """collection 已存在时 Milvus.__init__ 会立刻访问 ORM，需先注册连接。"""
    global _milvus_probe
    uri = connection_args.get("uri", "")
    if not is_remote_milvus_uri(uri):
        return
    _milvus_probe = MilvusClient(**connection_args)
    alias = _milvus_probe._using
    if connections.has_connection(alias):
        return
    connect_kwargs: dict[str, str] = {"uri": uri}
    token = connection_args.get("token")
    if token:
        connect_kwargs["token"] = token
    connections.connect(alias=alias, **connect_kwargs)


def get_guide_vector_store() -> Milvus:
    global _store
    if _store is None:
        uri = resolve_milvus_uri(settings.MILVUS_URI)
        connection_args: dict[str, str] = {"uri": uri}
        token = settings.MILVUS_TOKEN or ""
        if token and uri.startswith("http"):
            connection_args["token"] = token
        _prepare_milvus_orm_connection(connection_args)
        _store = Milvus(
            embedding_function=get_zhipu_embeddings(),
            collection_name=settings.MILVUS_COLLECTION,
            connection_args=connection_args,
            auto_id=True,
            enable_dynamic_field=True,
        )
    return _store
