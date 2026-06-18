"""解析 LLM 返回的 JSON 字符串。"""

import json
import re


def parse_llm_json(text: str) -> dict:
    """解析 JSON；自动剥离模型常加的 ```json ... ``` 包裹。"""
    if not text:
        return {}

    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
        cleaned = re.sub(r"\s*```$", "", cleaned)
    try:
        data = json.loads(cleaned)
        return data if isinstance(data, dict) else {}
    except json.JSONDecodeError:
        return {}
