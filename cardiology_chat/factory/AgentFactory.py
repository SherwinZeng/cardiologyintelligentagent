from langchain.agents import create_agent
from langchain.agents.middleware import SummarizationMiddleware
from langgraph.checkpoint.memory import InMemorySaver

from cardiology_chat.factory.LLMFactory import LLMModelFactory
from log.logger import get_app_logger
from cardiology_chat.prompt.general_prompt import system_general_prompt


class AgentFactory:
    _agents = {}

    def __init__(self):
        self.llm_factory = LLMModelFactory()
        self.logger = get_app_logger()

    def _get_or_create_agent(self, key, builder):
        if key not in self._agents:
            self._agents[key] = builder()
        return self._agents[key]

    def get_general_understanding_agent(self):
        return self._get_or_create_agent("general_understanding", lambda: create_agent(
            model=self.llm_factory.Get_LLM_Model(LLM="deepseek", model="deepseek-v4-flash"),
            system_prompt=system_general_prompt,
            checkpointer=InMemorySaver(),
            middleware=[
                SummarizationMiddleware(
                    model=self.llm_factory.Get_LLM_Model(),
                    trigger=("message", 6),
                    keep=("message", 3)
                )
            ]
        ))

    def get_reasoning_agent(self):
        return self._get_or_create_agent("reasoning", lambda: create_agent(
            model=self.llm_factory.Get_LLM_Model(
                LLM="deepseek",
                model="deepseek-v4-pro",
            ),
            system_prompt=system_reasoning_prompt,
        ))

    def get_multimodal_agent(self):
        return self._get_or_create_agent("multimodal", lambda: create_agent(
            model=self.llm_factory.Get_LLM_Model(LLM="qianwen"),
            system_prompt=system_multimodal_prompt,
        ))


agent_factory = AgentFactory()
