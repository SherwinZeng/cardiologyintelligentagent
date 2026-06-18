from rest_framework import serializers


class ChatSerializer(serializers.Serializer):
    uid = serializers.CharField(required=True, allow_blank=False, help_text="用户身份标识")
    session = serializers.CharField(
        required=True,
        allow_blank=False,
        help_text="会话 ID，作为 LangGraph thread_id 组成部分",
    )
    message = serializers.CharField(required=True, allow_blank=False)


class CheckpointDeleteSerializer(serializers.Serializer):
    uid = serializers.CharField(required=True, allow_blank=False)
    session = serializers.CharField(required=True, allow_blank=False)
