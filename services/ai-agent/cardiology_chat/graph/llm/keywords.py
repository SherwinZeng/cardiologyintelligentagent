"""确定性关键词检测（用于红旗、危急化验值等安全规则，非主路由）。"""

_NEGATION_PREFIXES = ("没有", "无", "不", "未")


def has_keyword(
    text: str,
    keywords: tuple[str, ...],
    *,
    case_insensitive: bool = False,
    negation_aware: bool = True,
) -> bool:
    """检查文本是否命中关键词。

    negation_aware=True 时，关键词前 4 字含否定词则不算命中（如「没有大汗」）。
    case_insensitive=True 时整段文本转小写再匹配（适合英文缩写 ECG/QRS）。
    """
    haystack = text.lower() if case_insensitive else text
    for keyword in keywords:
        needle = keyword.lower() if case_insensitive else keyword
        if needle not in haystack:
            continue
        if negation_aware:
            idx = haystack.find(needle)
            window = haystack[max(0, idx - 4):idx]
            if any(neg in window for neg in _NEGATION_PREFIXES):
                continue
        return True
    return False
