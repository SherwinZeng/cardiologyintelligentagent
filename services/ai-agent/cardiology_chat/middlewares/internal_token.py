from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed

from cardiology_chat.infra.redis_client import get_redis_client

INTERNAL_TOKEN_HEADER = "X-Internal-Token"
INTERNAL_TOKEN_KEY_PREFIX = "internal:token:"


class InternalTokenAuthentication(BaseAuthentication):
    def authenticate(self, request):
        token = request.headers.get(INTERNAL_TOKEN_HEADER)
        if not token or not token.strip():
            raise AuthenticationFailed("缺少内部通信令牌")

        token = token.strip()
        redis_key = f"{INTERNAL_TOKEN_KEY_PREFIX}{token}"
        stored = get_redis_client().get(redis_key)
        if not stored:
            raise AuthenticationFailed("无效或已过期的内部通信令牌")
        if stored.strip().strip('"') != "ok":
            raise AuthenticationFailed("内部通信令牌校验失败")
        get_redis_client().delete(redis_key)
        return None, token

    def authenticate_header(self, request):
        return INTERNAL_TOKEN_HEADER
