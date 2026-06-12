from rest_framework.views import exception_handler as drf_exception_handler
from rest_framework.response import Response
from rest_framework import status
from common.common_data.exception.chat_exception import ChatBusinessException
import logging

logger = logging.getLogger(__name__)


def custom_exception_handler(exc, context):
    response = drf_exception_handler(exc, context)

    if response is not None:
        # 可以统一格式化原生异常响应（可选）
        # 例如：将 response.data 包装成统一格式
        return Response({
            'code': getattr(exc, 'default_code', 'error'),
            'message': str(exc.detail) if hasattr(exc, 'detail') else response.data.get('detail', ''),
            'data': None
        }, status=response.status_code)

    # 处理自定义业务异常
    if isinstance(exc, ChatBusinessException):
        return Response({
            'code': exc.default_code,
            'message': exc.detail,
            'data': None
        }, status=exc.status_code)

    logger.error(f'Unhandled exception: {exc}', exc_info=True)
    return Response({
        'code': 'internal_error',
        'message': '服务器内部错误，请联系管理员',
        'data': None
    }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
