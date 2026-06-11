from rest_framework.exceptions import APIException

from common.common_data.response.ResponseCode import ResponseCode
from common.common_data.response.ResponseMessage import ResponseMessage


class ChatBusinessException(APIException):
    status_code = ResponseCode.SERVER_ERROR
    default_code = ResponseCode.SERVER_ERROR
    default_detail = "业务处理出错"

    def __init__(self, detail=None, code=None):
        if detail is not None:
            self.detail = detail
        if code is not None:
            self.code = code
        super().__init__(detail=detail, code=code)


class LLMServiceException(ChatBusinessException):
    status_code = ResponseCode.SERVICE_UNAVAILABLE
    default_code = ResponseCode.SERVICE_UNAVAILABLE
    default_detail = '大模型服务暂时不可用'

class InvalidPromptException(ChatBusinessException):
    status_code = ResponseCode.BAD_REQUEST
    default_code = ResponseCode.BAD_REQUEST
    default_detail = '请求参数错误'
