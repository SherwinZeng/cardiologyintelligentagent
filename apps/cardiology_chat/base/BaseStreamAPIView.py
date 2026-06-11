from django.http import StreamingHttpResponse
from rest_framework.renderers import JSONRenderer
from rest_framework.views import APIView

from cardiology_chat.renderers import EventStreamRenderer


class BaseStreamAPIView(APIView):
    renderer_classes = [EventStreamRenderer, JSONRenderer]

    def finalize_response(self, request, response, *args, **kwargs):
        if isinstance(response, StreamingHttpResponse):
            return response
        return super().finalize_response(request, response, *args, **kwargs)

    def stream_response(self, generator):
        response = StreamingHttpResponse(generator, content_type="text/event-stream")
        response["Cache-Control"] = "no-cache"
        response["X-Accel-Buffering"] = "no"
        return response
