"""LangGraph checkpointer（thread_id = uid:session），当前生产只使用 PostgreSQL。"""

import logging
from functools import lru_cache

from configuration import settings

logger = logging.getLogger(__name__)

_pool = None


def _build_postgres_saver():
    from langgraph.checkpoint.postgres import PostgresSaver
    from psycopg.rows import dict_row
    from psycopg_pool import ConnectionPool

    global _pool
    _pool = ConnectionPool(
        conninfo=settings.POSTGRES_CHECKPOINTER_URI,
        max_size=10,
        kwargs={"autocommit": True, "row_factory": dict_row},
    )
    saver = PostgresSaver(_pool)
    saver.setup()
    logger.info("LangGraph checkpointer 已连接 PostgreSQL")
    return saver


@lru_cache(maxsize=1)
def get_checkpointer():
    return _build_postgres_saver()


def build_thread_id(uid: str, session: str) -> str:
    return f"{uid}:{session}"


def delete_checkpoint_thread(uid: str, session: str) -> None:
    get_checkpointer().delete_thread(build_thread_id(uid, session))
