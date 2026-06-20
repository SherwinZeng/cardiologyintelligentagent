import unittest

from cardiology_chat.rag.guide_names import resolve_guide_display_name, unique_guide_display_names


class GuideNamesTests(unittest.TestCase):
    def test_hypertension_guide_chinese_name(self):
        name = resolve_guide_display_name(
            "NationalGuidelinesforPrimaryHypertensionPreventionandManagement2025Edition"
        )
        self.assertEqual(name, "国家基层高血压防治管理指南（2025版）")

    def test_unique_guide_display_names_preserves_order(self):
        stems = [
            "NationalGuidelinesforPrimaryHypertensionPreventionandManagement2025Edition",
            "NationalGuidelinesforPrimaryHypertensionPreventionandManagement2025Edition",
            "ChineseGuidelinesfortheDiagnosisandTreatmentofHeartFailure2024",
        ]
        self.assertEqual(
            unique_guide_display_names(stems),
            [
                "国家基层高血压防治管理指南（2025版）",
                "中国心力衰竭诊断和治疗指南（2024）",
            ],
        )


if __name__ == "__main__":
    unittest.main()
