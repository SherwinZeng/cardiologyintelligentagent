"""调用 DeepSeek Flash 并组装标准输出字段。"""

from langchain_core.messages import HumanMessage, SystemMessage

from cardiology_chat.factory.LLMFactory import LLMModelFactory
from cardiology_chat.graph.dialogue_core import ContextBuilder
from cardiology_chat.graph.llm.json_parser import parse_llm_json
from cardiology_chat.graph.llm.messages import conversation_messages_for_llm
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.llm.shared import with_conversation_rules

STANDARD_LLM_KEYS = ("urgency", "explanation", "advice", "disclaimer")

DEFAULT_RETRY_TEMPERATURES = (0.3, 0.1)

_flash_llm_cache: dict[float, object] = {}


def get_flash_llm(temperature: float = 0.3):
    if temperature not in _flash_llm_cache:
        _flash_llm_cache[temperature] = LLMModelFactory().Get_LLM_Model(temperature=temperature)
    return _flash_llm_cache[temperature]


def is_valid_llm_data(data: dict, required_keys: tuple[str, ...] = STANDARD_LLM_KEYS) -> bool:
    return all(
        isinstance(data.get(key), str) and data.get(key).strip()
        for key in required_keys
    )


def invoke_llm_json(
    state: CardiologyState,
    system_prompt: str,
    *,
    required_keys: tuple[str, ...] = STANDARD_LLM_KEYS,
    temperature: float = 0.3,
    user_text: str | None = None,
    use_conversation_rules: bool = False,
) -> dict:
    """调用 Flash 并解析 JSON。

    - user_text 为 None：多轮，带最近 messages（默认）
    - user_text 有值：单轮 system + 该条 user
    - use_conversation_rules：追加 prompts/llm/shared.py 中的多轮规则
    """
    prompt = with_conversation_rules(system_prompt) if use_conversation_rules else system_prompt
    context_prompt = ContextBuilder.as_system_prompt(state)
    if context_prompt:
        prompt = f"{prompt}\n\n{context_prompt}"
    try:
        llm = get_flash_llm(temperature)
        messages = [SystemMessage(content=prompt)]
        if user_text is not None:
            messages.append(HumanMessage(content=user_text))
        else:
            messages.extend(conversation_messages_for_llm(state))
        response = llm.invoke(messages)
        data = parse_llm_json(response.content)
        return data if is_valid_llm_data(data, required_keys) else {}
    except Exception:
        return {}


def invoke_llm_json_with_retry(
    state: CardiologyState,
    system_prompt: str,
    *,
    required_keys: tuple[str, ...] = STANDARD_LLM_KEYS,
    temperatures: tuple[float, ...] = DEFAULT_RETRY_TEMPERATURES,
    user_text: str | None = None,
    use_conversation_rules: bool = False,
) -> dict:
    """按 temperatures 依次重试，全部失败返回 {}。"""
    for temperature in temperatures:
        data = invoke_llm_json(
            state,
            system_prompt,
            required_keys=required_keys,
            temperature=temperature,
            user_text=user_text,
            use_conversation_rules=use_conversation_rules,
        )
        if data:
            return data
    return {}


def pick_llm_field(data: dict, key: str, fallback: str) -> str:
    value = data.get(key)
    return value if isinstance(value, str) and value.strip() else fallback


def build_standard_llm_fields(
    llm_data: dict | None,
    *,
    triage_fallback: str,
    impression_fallback: str,
    advice_fallback: str,
    disclaimer_fallback: str,
) -> dict:
    data = llm_data or {}
    return {
        "triage_level": pick_llm_field(data, "urgency", triage_fallback),
        "clinical_impression": pick_llm_field(data, "explanation", impression_fallback),
        "management_advice": pick_llm_field(data, "advice", advice_fallback),
        "medical_disclaimer": pick_llm_field(data, "disclaimer", disclaimer_fallback),
    }
