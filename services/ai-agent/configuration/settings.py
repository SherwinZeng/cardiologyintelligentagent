import os
import sys
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent

sys.path.insert(0, str(BASE_DIR))

load_dotenv(BASE_DIR / ".env")

SECRET_KEY = os.environ["DJANGO_SECRET_KEY"]
DEBUG = os.getenv("DJANGO_DEBUG", "False").lower() in {"1", "true", "yes", "on"}
ALLOWED_HOSTS = [
    host.strip()
    for host in os.getenv("DJANGO_ALLOWED_HOSTS", "").split(",")
    if host.strip()
]

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY")
QIANWEN_API_KEY = os.getenv("QIANWEN_API_KEY")
UNSTRUCTURED_API_KEY = os.getenv("UNSTRUCTURED_API_KEY")

DEFAULT_TEMPERATURE = float(os.getenv("DEFAULT_TEMPERATURE", "0.7"))
DEFAULT_MAX_TOKENS = int(os.getenv("DEFAULT_MAX_TOKENS", "2048"))

ZHIPU_API_KEY = os.getenv("ZHIPU_API_KEY")
ZHIPU_EMBEDDING_MODEL = os.getenv("ZHIPU_EMBEDDING_MODEL", "embedding-3")
ZHIPU_EMBEDDING_DIMENSIONS = int(os.getenv("ZHIPU_EMBEDDING_DIMENSIONS", "1024"))

MILVUS_URI = os.getenv("MILVUS_URI", "http://127.0.0.1:19530")
MILVUS_TOKEN = os.getenv("MILVUS_TOKEN", "")
MILVUS_COLLECTION = os.getenv("MILVUS_COLLECTION", "cardiology_guides")
RAG_ENABLED = os.getenv("RAG_ENABLED", "true").lower() in {"1", "true", "yes", "on"}
RAG_TOP_K = int(os.getenv("RAG_TOP_K", "5"))
RAG_FETCH_K = int(os.getenv("RAG_FETCH_K", "24"))
RAG_VECTOR_WEIGHT = float(os.getenv("RAG_VECTOR_WEIGHT", "0.35"))
RAG_KEYWORD_WEIGHT = float(os.getenv("RAG_KEYWORD_WEIGHT", "0.65"))

REDIS_HOST = os.getenv("REDIS_HOST", "127.0.0.1")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_DB = int(os.getenv("REDIS_DB", "0"))

POSTGRES_HOST = os.getenv("POSTGRES_HOST", "127.0.0.1")
POSTGRES_PORT = int(os.getenv("POSTGRES_PORT", "5432"))
POSTGRES_USER = os.getenv("POSTGRES_USER", "cardiology")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD", "cardiology")
POSTGRES_DB = os.getenv("POSTGRES_DB", "cardiology")
POSTGRES_CHECKPOINTER_URI = os.getenv(
    "POSTGRES_CHECKPOINTER_URI",
    f"postgresql://{POSTGRES_USER}:{POSTGRES_PASSWORD}@{POSTGRES_HOST}:{POSTGRES_PORT}/{POSTGRES_DB}",
)

LANGCHAIN_TRACING_V2 = os.getenv("LANGCHAIN_TRACING_V2", "false").lower() in {"1", "true", "yes", "on"}
LANGCHAIN_API_KEY = os.getenv("LANGCHAIN_API_KEY", "")
LANGCHAIN_PROJECT = os.getenv("LANGCHAIN_PROJECT", "cardiology-intelligent-agent")

INSTALLED_APPS = [
    "rest_framework",
    "cardiology_chat",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.middleware.common.CommonMiddleware",
]

ROOT_URLCONF = "configuration.urls"

WSGI_APPLICATION = "configuration.wsgi.application"

DATABASES = {}

REST_FRAMEWORK = {
    "EXCEPTION_HANDLER": "configuration.handler.exception_handler.custom_exception_handler",
    "DEFAULT_RENDERER_CLASSES": [
        "rest_framework.renderers.JSONRenderer",
    ],
    "DEFAULT_PARSER_CLASSES": [
        "rest_framework.parsers.JSONParser",
    ],
    "DEFAULT_AUTHENTICATION_CLASSES": [],
    "DEFAULT_PERMISSION_CLASSES": [],
    "UNAUTHENTICATED_USER": None,
}

LANGUAGE_CODE = "en-us"
TIME_ZONE = "UTC"
USE_I18N = True
USE_TZ = True
