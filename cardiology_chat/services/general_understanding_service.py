from langchain_core.messages import HumanMessage
from langchain_core.output_parsers import JsonOutputParser

from common.common_data.exception.chat_exception import InvalidPromptException
from cardiology_chat.serializers.chat_serializer import ChatSerializer


def general_understanding_service(request, cardiology_general_understanding_agent):
    chat_serializer = ChatSerializer(data=request.data)
    if not chat_serializer.is_valid():
        raise InvalidPromptException(chat_serializer.errors)
    message = [
        HumanMessage(content=chat_serializer.validated_data["message"])
    ]
    agent_result = cardiology_general_understanding_agent.invoke({"messages": message})
    last_message = agent_result["messages"][-1]
    return JsonOutputParser().parse(last_message.content)
