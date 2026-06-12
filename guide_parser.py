from pathlib import Path
from typing import List

from langchain_core.documents import Document
from langchain_unstructured import UnstructuredLoader

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
            max_characters=1200,
            new_after_n_chars=1500,
            combine_text_under_n_chars=300,
        )
        documents: List[Document] = []
        for doc in loader.lazy_load():
            documents.append(self._enrich_metadata(doc))
        self.logger.info(f"知识库PDF解析成功,一共{len(documents)} documents")
        return documents

    def _enrich_metadata(self, doc: Document) -> Document:
        source = doc.metadata.get("source") or doc.metadata.get("filename") or ""
        if source:
            doc.metadata["guide_name"] = Path(str(source)).stem
        return doc


if __name__ == "__main__":
    parser = GuidePDFParser()
    docs = parser.parse_guide_pdf_parser()
    print(f"解析完成: {len(docs)} 个文本块, PDF 数量: {len(parser.pdf_paths)}")

