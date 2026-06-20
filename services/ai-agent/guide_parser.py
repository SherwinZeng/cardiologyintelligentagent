from pathlib import Path
from typing import List

from langchain_core.documents import Document
from langchain_unstructured import UnstructuredLoader

from cardiology_chat.rag.text_normalize import normalize_pdf_text
from configuration import settings
from log.logger import get_app_logger

GUIDE_DIR = Path(__file__).resolve().parent / "guide"


class GuidePDFParser:
    def __init__(self, guide_dir: Path | None = None):
        self.logger = get_app_logger()
        guide_dir = guide_dir or GUIDE_DIR
        self.guide_dir = guide_dir
        self.pdf_paths = sorted(guide_dir.glob("*.pdf"))
        if not self.pdf_paths:
            raise FileNotFoundError(f"目录下没有 PDF: {guide_dir}")
        if not settings.UNSTRUCTURED_API_KEY:
            raise ValueError("请在 .env 中配置 UNSTRUCTURED_API_KEY")

    def parse_guide_pdf_parser(self, pdf_paths: List[Path] | None = None) -> List[Document]:
        paths = pdf_paths or self.pdf_paths
        loader = UnstructuredLoader(
            file_path=paths,
            partition_via_api=True,
            api_key=settings.UNSTRUCTURED_API_KEY,
            strategy="fast",
            languages=["chi_sim", "eng"],
            chunking_strategy="basic",
            max_characters=800,
            new_after_n_chars=650,
            combine_text_under_n_chars=150,
        )
        documents: List[Document] = []
        for doc in loader.lazy_load():
            documents.append(self._enrich_metadata(doc))
        self.logger.info(f"知识库PDF解析成功,一共{len(documents)} documents")
        return documents

    def _enrich_metadata(self, doc: Document) -> Document:
        source = doc.metadata.get("source") or doc.metadata.get("filename") or ""
        guide_name = Path(str(source)).stem if source else ""
        # 只保留简单标量字段，避免 Unstructured 的 list/array metadata 导致 Milvus 建库失败
        metadata: dict[str, str | int] = {}
        if guide_name:
            metadata["guide_name"] = guide_name[:512]
        page = doc.metadata.get("page_number")
        if page is not None:
            try:
                metadata["page_number"] = int(page)
            except (TypeError, ValueError):
                pass
        content = normalize_pdf_text(doc.page_content or "")
        return Document(page_content=content, metadata=metadata)

