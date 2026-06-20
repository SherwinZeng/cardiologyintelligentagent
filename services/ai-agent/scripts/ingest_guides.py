from __future__ import annotations

import argparse
import os
import pickle
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "configuration.settings")

from cardiology_chat.rag.text_normalize import normalize_document  # noqa: E402
from guide_parser import GuidePDFParser  # noqa: E402
from log.logger import get_app_logger  # noqa: E402

DEFAULT_CACHE = BASE_DIR / ".cache" / "parsed_guides.pkl"


def _load_cached_docs(cache_path: Path):
    if not cache_path.exists():
        raise SystemExit(f"缓存不存在: {cache_path}，请先不加 --skip-parse 运行一次")
    with cache_path.open("rb") as handle:
        docs = pickle.load(handle)
    if not docs:
        raise SystemExit(f"缓存为空: {cache_path}")
    return docs


def _save_cached_docs(cache_path: Path, docs) -> None:
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    with cache_path.open("wb") as handle:
        pickle.dump(docs, handle)


def main() -> None:
    parser = argparse.ArgumentParser(description="心血管指南 PDF 入库 Milvus")
    parser.add_argument(
        "--recreate",
        action="store_true",
        help="删除已有 collection 后重新入库",
    )
    parser.add_argument(
        "--skip-parse",
        action="store_true",
        help="跳过 Unstructured PDF 解析，使用本地缓存",
    )
    parser.add_argument(
        "--cache-file",
        type=Path,
        default=DEFAULT_CACHE,
        help=f"解析结果缓存路径（默认 {DEFAULT_CACHE}）",
    )
    parser.add_argument(
        "--pdf",
        type=str,
        default="",
        help="只入库指定 PDF（文件名或路径片段，如 HeartFailure2024）",
    )
    args = parser.parse_args()
    logger = get_app_logger()

    from configuration import settings  # noqa: WPS433
    from cardiology_chat.rag.guide_store import get_guide_vector_store  # noqa: WPS433
    from pymilvus import MilvusClient  # noqa: WPS433

    if not settings.ZHIPU_API_KEY:
        raise SystemExit("缺少 ZHIPU_API_KEY，请在 services/ai-agent/.env 配置")

    if args.recreate:
        from cardiology_chat.rag.guide_store import reset_guide_vector_store
        from cardiology_chat.rag.milvus_uri import is_remote_milvus_uri, resolve_milvus_uri

        uri = resolve_milvus_uri(settings.MILVUS_URI)
        if is_remote_milvus_uri(settings.MILVUS_URI):
            client_kwargs: dict[str, str] = {"uri": uri}
            if settings.MILVUS_TOKEN:
                client_kwargs["token"] = settings.MILVUS_TOKEN
            client = MilvusClient(**client_kwargs)
            if client.has_collection(settings.MILVUS_COLLECTION):
                client.drop_collection(settings.MILVUS_COLLECTION)
                logger.info("已删除 collection: %s", settings.MILVUS_COLLECTION)
        else:
            db_path = Path(uri)
            for suffix in ("", ".lock"):
                candidate = Path(f"{db_path}{suffix}")
                if candidate.exists():
                    candidate.unlink()
                    logger.info("已删除 Milvus Lite 文件: %s", candidate)
        reset_guide_vector_store()

    if args.skip_parse:
        docs = _load_cached_docs(args.cache_file)
        logger.info("使用缓存，共 %s 个文本块 | path=%s", len(docs), args.cache_file)
    else:
        if not settings.UNSTRUCTURED_API_KEY:
            raise SystemExit("缺少 UNSTRUCTURED_API_KEY，PDF 解析需要；或加 --skip-parse 使用缓存")
        parser_obj = GuidePDFParser()
        pdf_paths = parser_obj.pdf_paths
        if args.pdf:
            needle = args.pdf.strip().lower()
            pdf_paths = [path for path in pdf_paths if needle in path.name.lower()]
            if not pdf_paths:
                raise SystemExit(f"未找到匹配 PDF: {args.pdf}")
            logger.info("仅解析 %s 个 PDF: %s", len(pdf_paths), [p.name for p in pdf_paths])
        docs = parser_obj.parse_guide_pdf_parser(pdf_paths=pdf_paths)

    docs = [normalize_document(doc) for doc in docs]
    docs = [doc for doc in docs if (doc.page_content or "").strip()]
    if not args.skip_parse:
        _save_cached_docs(args.cache_file, docs)
        logger.info("PDF 解析完成，共 %s 个文本块，已缓存到 %s", len(docs), args.cache_file)

    store = get_guide_vector_store()
    batch_size = 32
    all_ids: list[str] = []
    for start in range(0, len(docs), batch_size):
        batch = docs[start : start + batch_size]
        batch_ids = store.add_documents(batch)
        all_ids.extend(batch_ids)
        logger.info("已入库 %s / %s", len(all_ids), len(docs))

    logger.info("Milvus 入库完成 | collection=%s | chunks=%s", settings.MILVUS_COLLECTION, len(all_ids))
    print(f"OK: ingested {len(all_ids)} chunks into {settings.MILVUS_COLLECTION}")


if __name__ == "__main__":
    main()
