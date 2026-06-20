from rest_framework.response import Response
from rest_framework.views import APIView

from cardiology_chat.middlewares.internal_token import InternalTokenAuthentication
from cardiology_chat.serializers.chat_serializer import (
    ChatSerializer,
    CheckpointDeleteSerializer,
    SessionSummarySerializer,
)
from cardiology_chat.services.chat_graph_service import delete_session_checkpoint, invoke_general_understanding
from cardiology_chat.services.session_summary_service import summarize_session
from common.common_data.response.ResponseCode import ResponseCode
from common.common_data.response.ResponseMessage import ResponseMessage


class CardiologyGeneralUnderstandingView(APIView):
    authentication_classes = [InternalTokenAuthentication]

    def post(self, request):
        serializer = ChatSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {"code": ResponseCode.BAD_REQUEST, "message": str(serializer.errors), "data": None},
                status=ResponseCode.BAD_REQUEST,
            )
        data = serializer.validated_data
        result = invoke_general_understanding(
            uid=data["uid"],
            session=data["session"],
            message=data["message"],
        )
        return Response(
            {"code": ResponseCode.SUCCESS, "message": ResponseMessage.SUCCESS, "data": result},
            status=ResponseCode.SUCCESS,
        )

    def delete(self, request):
        serializer = CheckpointDeleteSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {"code": ResponseCode.BAD_REQUEST, "message": str(serializer.errors), "data": None},
                status=ResponseCode.BAD_REQUEST,
            )
        data = serializer.validated_data
        delete_session_checkpoint(uid=data["uid"], session=data["session"])
        return Response(
            {"code": ResponseCode.SUCCESS, "message": ResponseMessage.SUCCESS, "data": None},
            status=ResponseCode.SUCCESS,
        )


class CardiologySessionSummaryView(APIView):
    authentication_classes = [InternalTokenAuthentication]

    def post(self, request):
        serializer = SessionSummarySerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {"code": ResponseCode.BAD_REQUEST, "message": str(serializer.errors), "data": None},
                status=ResponseCode.BAD_REQUEST,
            )
        data = serializer.validated_data
        message_count = data.get("message_count", data.get("messageCount"))
        result = summarize_session(data["messages"], message_count)
        return Response(
            {"code": ResponseCode.SUCCESS, "message": ResponseMessage.SUCCESS, "data": result},
            status=ResponseCode.SUCCESS,
        )


class CardiologyReasoningView(APIView):
    pass


class CardiologyMultimodalView(APIView):
    pass
