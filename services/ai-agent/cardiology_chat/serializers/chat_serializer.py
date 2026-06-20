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


class SessionSummaryMessageSerializer(serializers.Serializer):
    role = serializers.CharField(required=True, allow_blank=False)
    content = serializers.CharField(required=True, allow_blank=True)
    urgency = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    advice = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    disclaimer = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    created_at = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    createdAt = serializers.CharField(required=False, allow_blank=True, allow_null=True)


class SessionSummarySerializer(serializers.Serializer):
    uid = serializers.CharField(required=True, allow_blank=False)
    session = serializers.CharField(required=True, allow_blank=False)
    message_count = serializers.IntegerField(required=False, min_value=0)
    messageCount = serializers.IntegerField(required=False, min_value=0)
    messages = SessionSummaryMessageSerializer(many=True, required=True)
