from django.urls import path

from cardiology_chat.views import (
    CardiologyGeneralUnderstandingView,
    CardiologyMultimodalView,
    CardiologyReasoningView,
)

urlpatterns = [
    path(
        "general-understanding/",
        CardiologyGeneralUnderstandingView.as_view(),
        name="cardiology-general-understanding",
    ),
    path(
        "reasoning/",
        CardiologyReasoningView.as_view(),
        name="cardiology-reasoning",
    ),
    path(
        "multimodal/",
        CardiologyMultimodalView.as_view(),
        name="cardiology-multimodal",
    ),
]
