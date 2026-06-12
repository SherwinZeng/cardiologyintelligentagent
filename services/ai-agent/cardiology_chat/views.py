from django.http import JsonResponse

from rest_framework.views import APIView

from cardiology_chat.factory.AgentFactory import agent_factory
from cardiology_chat.services.general_understanding_service import general_understanding_service
from common.common_data.response.ResponseCode import ResponseCode
from common.common_data.response.ResponseMessage import ResponseMessage


class CardiologyGeneralUnderstandingView(APIView):
    def post(self, request):
        general_model_result = general_understanding_service(
            request,
            agent_factory.get_general_understanding_agent(),
        )
        return JsonResponse(
            {"code": ResponseCode.SUCCESS, "message": ResponseMessage.SUCCESS, "data": general_model_result},
            status=ResponseCode.SUCCESS,
        )


class CardiologyReasoningView(APIView):
    pass


class CardiologyMultimodalView(APIView):
    pass
