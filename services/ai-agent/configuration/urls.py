from django.urls import include, path

urlpatterns = [
    path("api/cardiology/", include("cardiology_chat.urls")),
]
