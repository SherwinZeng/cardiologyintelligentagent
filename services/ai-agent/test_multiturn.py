from cardiology_chat.services.chat_graph_service import invoke_general_understanding


def run_turn(session: str, message: str, label: str):
    result = invoke_general_understanding("u1", session, message)
    print(f"--- {label} ---")
    print(f"输入: {message!r}")
    print(f"  urgency: {result['urgency']}")
    print(f"  explanation[:100]: {result['explanation'][:100]}...")
    print()


print("=" * 50)
print("症状多轮")
session1 = "multi-symptom-1"
run_turn(session1, "我胸口疼", "第1轮")
run_turn(session1, "疼了两天，活动后加重，没有大汗", "第2轮")

print("=" * 50)
print("既往史多轮")
session2 = "multi-history-1"
run_turn(session2, "我有高血压", "第1轮")
run_turn(session2, "平时大概140/90，在吃降压药", "第2轮")
