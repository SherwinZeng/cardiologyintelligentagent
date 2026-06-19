"""稳定事实记忆键（checkpointer 内 conversation_memory）。"""

from typing import Literal

MEMORY_KEYS = frozenset(
    {
        "display_name",
        "age",
        "sex",
        "chronic_conditions",
        "allergies",
        "long_term_meds",
    }
)

SexHint = Literal["male", "female", "unknown"]
