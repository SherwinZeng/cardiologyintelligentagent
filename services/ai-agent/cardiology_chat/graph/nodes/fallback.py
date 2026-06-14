from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.fallback import NON_CARDIO_FALLBACK


def medical_fallback_response_node(state: CardiologyState) -> dict:
    return {
        "clinical_impression": NON_CARDIO_FALLBACK,
        "management_advice": "",
        "medical_disclaimer": "",
        "triage_level": "",
    }
