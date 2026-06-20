import unittest

from langchain_core.documents import Document

from cardiology_chat.rag.guide_rerank import detect_query_topics, rerank_scored_documents
from cardiology_chat.rag.text_normalize import normalize_pdf_text


class TextNormalizeTests(unittest.TestCase):
    def test_removes_spaces_between_cjk_chars(self):
        raw = "平 衡阴阳，对 高血压的防治"
        self.assertEqual(normalize_pdf_text(raw), "平衡阴阳，对高血压的防治")


class GuideRerankTests(unittest.TestCase):
    def test_detect_diet_topic(self):
        self.assertIn("diet", detect_query_topics("高血压饮食注意什么"))

    def test_rerank_prefers_diet_chunk_over_assessment(self):
        query = "高血压饮食注意什么"
        diet_doc = Document(
            page_content="合理膳食，限盐，增加富含钾和膳食纤维的食物摄入。",
            metadata={"guide_name": "hypertension"},
        )
        noise_doc = Document(
            page_content="评估内容包括病史、体格检查及辅助检查。合并症包括冠心病。",
            metadata={"guide_name": "hypertension"},
        )
        candidates = [
            (noise_doc, 0.86),
            (noise_doc, 0.91),
            (diet_doc, 1.01),
        ]
        ranked = rerank_scored_documents(query, candidates, final_k=1)
        self.assertEqual(len(ranked), 1)
        self.assertIn("合理膳食", ranked[0].page_content)


if __name__ == "__main__":
    unittest.main()
