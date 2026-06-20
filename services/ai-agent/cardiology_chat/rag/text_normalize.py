"""PDF 解析后文本清洗，提升 embedding 与关键词匹配质量。"""

from __future__ import annotations

import re

from langchain_core.documents import Document

# Unstructured 中文 PDF 常在字间插入空格，如「平 衡阴阳」
_CJK_SPACE = re.compile(r"(?<=[\u4e00-\u9fff])\s+(?=[\u4e00-\u9fff])")
_MULTI_SPACE = re.compile(r"[ \t]{2,}")
_BLANK_LINES = re.compile(r"\n{3,}")


def normalize_pdf_text(text: str) -> str:
    if not text:
        return ""
    cleaned = _CJK_SPACE.sub("", text)
    cleaned = _MULTI_SPACE.sub(" ", cleaned)
    cleaned = _BLANK_LINES.sub("\n\n", cleaned)
    return cleaned.strip()


def normalize_document(doc: Document) -> Document:
    content = normalize_pdf_text(doc.page_content or "")
    return Document(page_content=content, metadata=dict(doc.metadata or {}))
