from django.urls import path

from cardiology_chat.views import (
    CardiologyCheckpointDeleteView,
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
        "checkpoint/delete/",
        CardiologyCheckpointDeleteView.as_view(),
        name="cardiology-checkpoint-delete",
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
