from django.http import JsonResponse

from cardiology_chat.base.BaseCachedLLM import BaseCachedLLM
from cardiology_chat.base.BaseStreamAPIView import BaseStreamAPIView
from cardiology_chat.services.general_understanding_service import general_understanding_service
from common.common_data.response.ResponseCode import ResponseCode
from common.common_data.response.ResponseMessage import ResponseMessage


class CardiologyGeneralUnderstandingView(BaseStreamAPIView, BaseCachedLLM):
    def post(self, request):
        general_model_result = general_understanding_service(request, self.general_model)
        return JsonResponse({"code": ResponseCode.SUCCESS, "message": ResponseMessage.SUCCESS, "data": general_model_result},
                            status=ResponseCode.SUCCESS)


class CardiologyReasoningView(BaseStreamAPIView, BaseCachedLLM):
    pass


class CardiologyMultimodalView(BaseStreamAPIView, BaseCachedLLM):
    pass
