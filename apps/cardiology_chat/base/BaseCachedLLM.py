from cardiology_chat.factory.LLMFactory import LLMModelFactory


class BaseCachedLLM:
    _general_model = None
    _reasoning_model = None
    _multimodal_model = None

    @property
    def general_model(self):
        if self.__class__._general_model is None:
            factory = LLMModelFactory()
            self.__class__._general_model = factory.Get_LLM_Model(
                model="deepseek-v4-flash", temperature=0.7
            )
        return self.__class__._general_model

    @property
    def reasoning_model(self):
        if self.__class__._reasoning_model is None:
            factory = LLMModelFactory()
            self.__class__._reasoning_model = factory.Get_LLM_Model(
                model="deepseek-v4-pro", temperature=0.7
            )
        return self.__class__._reasoning_model

    @property
    def multimodal_model(self):
        if self.__class__._multimodal_model is None:
            factory = LLMModelFactory()
            self.__class__._multimodal_model = factory.Get_LLM_Model(
                LLM="qwen", model="qwen3.7-plus", temperature=0.5
            )
        return self.__class__._multimodal_model
