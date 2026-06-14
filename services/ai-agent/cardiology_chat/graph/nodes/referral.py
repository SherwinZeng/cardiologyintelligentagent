from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.referral import (
    REFERRAL_ADVICE,
    REFERRAL_LEVELS,
    REFERRAL_DEPARTMENTS,
    REFERRAL_NOTES,
    REFERRAL_EXTRA_TIPS,
    REFERRAL_WHEN_TO_ER,
)


def physician_referral_node(state: CardiologyState) -> dict:
    triage = state.get("triage_level") or "green"

    referral = REFERRAL_ADVICE.format(
        referral_level=REFERRAL_LEVELS.get(triage, REFERRAL_LEVELS["green"]),
        department=REFERRAL_DEPARTMENTS.get(triage, REFERRAL_DEPARTMENTS["green"]),
        referral_note=REFERRAL_NOTES.get(triage, REFERRAL_NOTES["green"]),
    )

    advice = state.get("management_advice", "")
    extra = REFERRAL_EXTRA_TIPS
    if triage in ("yellow", "red"):
        extra = f"{REFERRAL_EXTRA_TIPS}\n\n{REFERRAL_WHEN_TO_ER}"

    return {
        "management_advice": f"{advice}\n\n{referral}\n\n{extra}".strip(),
    }
