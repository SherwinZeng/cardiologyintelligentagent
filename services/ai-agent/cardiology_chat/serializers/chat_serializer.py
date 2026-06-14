from rest_framework import serializers


class ChatSerializer(serializers.Serializer):
    uid = serializers.CharField(
        required=True,
        allow_blank=False,
        help_text="用户身份标识，仅用于验证，不参与多轮记忆",
    )
    session = serializers.CharField(
        required=True,
        allow_blank=False,
        help_text="会话 ID，作为 LangGraph thread_id，多轮记忆键",
    )
    message = serializers.CharField(required=True, allow_blank=False)
