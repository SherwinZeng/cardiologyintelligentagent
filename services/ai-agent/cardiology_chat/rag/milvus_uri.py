from pathlib import Path

from configuration.settings import BASE_DIR


def resolve_milvus_uri(raw: str) -> str:
    """http(s) → Docker Milvus；否则 → Milvus Lite 本地文件路径。"""
    uri = raw.strip()
    if uri.startswith("http://") or uri.startswith("https://"):
        return uri
    path = Path(uri)
    if not path.is_absolute():
        path = BASE_DIR / path
    path.parent.mkdir(parents=True, exist_ok=True)
    return str(path)


def is_remote_milvus_uri(uri: str) -> bool:
    return uri.startswith("http://") or uri.startswith("https://")
