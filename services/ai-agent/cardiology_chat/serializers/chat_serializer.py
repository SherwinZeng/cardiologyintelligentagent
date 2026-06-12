from rest_framework import serializers

class ChatSerializer(serializers.Serializer):
    token = serializers.CharField(required=False, allow_blank=True)
    message = serializers.CharField(required=True, allow_blank=False)
