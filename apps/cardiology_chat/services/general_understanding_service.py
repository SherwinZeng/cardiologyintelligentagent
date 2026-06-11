from langchain_core.messages import SystemMessage, HumanMessage
from langchain_core.output_parsers import JsonOutputParser

from common.common_data.exception.chat_exception import InvalidPromptException
from cardiology_chat.serializers.chat_serializer import ChatSerializer
from cardiology_chat.prompt.general_prompt import system_general_prompt


def general_understanding_service(request, general_model):
    chat_serializer = ChatSerializer(data=request.data)
    if not chat_serializer.is_valid():
        raise InvalidPromptException(chat_serializer.errors)
    message = [
        SystemMessage(content=system_general_prompt),
        HumanMessage(content=chat_serializer.validated_data["message"])
    ]
    general_mode_result = general_model.invoke(message)
    return JsonOutputParser().parse(general_mode_result.content)
