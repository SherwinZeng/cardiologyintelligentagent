from langchain_community.chat_models import ChatTongyi
from langchain_deepseek import ChatDeepSeek

from configuration import settings
from log.logger import get_app_logger


class LLMModelFactory:
    def __init__(self):
        self.default_temperature = getattr(settings, "DEFAULT_TEMPERATURE", 0.7)
        self.default_max_tokens = getattr(settings, "DEFAULT_MAX_TOKENS", 2048)
        self.deepseek_api_key = getattr(settings, "DEEPSEEK_API_KEY", None)
        self.qianwen_api_key = getattr(settings, "QIANWEN_API_KEY", None)
        self.logger = get_app_logger()

    def Get_LLM_Model(
        self,
        LLM: str = "deepseek",
        model: str = "deepseek-v4-flash",
        temperature=None,
        max_tokens=None,
    ):
        if LLM == "deepseek":
            if model == "deepseek-v4-flash":
                self.logger.info(
                    f"当前使用的是{LLM}的{model},适用于普通医疗对话场景,temperature={temperature},max_tokens={max_tokens}"
                )
                return ChatDeepSeek(
                    api_key=self.deepseek_api_key,
                    model="deepseek-v4-flash",
                    temperature=temperature,
                    max_tokens=max_tokens,
                )
            self.logger.info(
                f"当前使用的是{LLM}的{model},适用于深度医疗推理场景,temperature={temperature},max_tokens={max_tokens}"
            )
            return ChatDeepSeek(
                api_key=self.deepseek_api_key,
                model=model,
                temperature=temperature,
                max_tokens=max_tokens,
            )
        self.logger.info("当前使用的是通义千问的qwen3.7-plus,适用于识别ECG等多模态医疗解读")
        return ChatTongyi(
            api_key=self.qianwen_api_key,
            model="qwen3.7-plus",
        )