from langchain_core.messages import HumanMessage
from cardiology_chat.graph import cardiology_graph
from cardiology_chat.graph.state import empty_cardiology_state

def run(msg: str):
    state = empty_cardiology_state()
    state["messages"] = [HumanMessage(content=msg)]
    result = cardiology_graph.invoke(
        state,
        config={"configurable": {"thread_id": "test"}},
    )
    print(f"输入: {msg!r}")
    print(f"  route: {result['route']}")
    print(f"  triage: {result['triage_level']}")
    print(f"  impression: {result['clinical_impression'][:40]}...")
    print(f"  has_risk: {'心血管风险初步分层' in result['clinical_impression']}")
    print(f"  impression_tail: ...{result['clinical_impression'][-60:]}")
    print()
    print()

run("1234567")
run("我胸口疼")
run("我胸口剧烈胸痛伴大汗晕厥")
run("我有高血压")
run("我有糖尿病和冠心病")
run("帮我看看这份心电图报告")
run("肌钙蛋白升高是什么意思")
run("帮我看看这份心电图报告")
run("肌钙蛋白升高是什么意思")
run("肌钙蛋白升高是什么意思")