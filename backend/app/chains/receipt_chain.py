"""LangGraph pipeline: vision-extract -> categorize -> anomaly-check.

The graph takes a base64-encoded receipt image and returns a structured
ScanResult. No OCR binary needed — the Groq vision model reads the image
directly.
"""
from __future__ import annotations

import json
from typing import TypedDict

from langchain_core.messages import HumanMessage, SystemMessage
from langchain_groq import ChatGroq
from langgraph.graph import END, StateGraph

from app.config import get_settings
from app.models.schemas import ExtractedReceipt, LineItem, ScanResult

_settings = get_settings()

CATEGORIES = [
    "Food & Dining",
    "Groceries",
    "Transport",
    "Shopping",
    "Utilities",
    "Health",
    "Entertainment",
    "Travel",
    "Other",
]

_vision_llm = ChatGroq(
    model=_settings.GROQ_VISION_MODEL,
    api_key=_settings.GROQ_API_KEY,
    temperature=0,
)
_text_llm = ChatGroq(
    model=_settings.GROQ_TEXT_MODEL,
    api_key=_settings.GROQ_API_KEY,
    temperature=0,
)


class GraphState(TypedDict):
    image_b64: str
    category_avgs: dict[str, float]  # user's historical avg spend per category
    extracted: ExtractedReceipt
    result: ScanResult


def _extract_node(state: GraphState) -> dict:
    prompt = (
        "You are a receipt parser. Read this receipt image and return STRICT JSON "
        "with keys: merchant_name (string), receipt_date (YYYY-MM-DD or null), "
        "total_amount (number), currency (3-letter code, default INR), "
        "items (array of {name, amount}). Return ONLY the JSON, no prose."
    )
    msg = HumanMessage(
        content=[
            {"type": "text", "text": prompt},
            {
                "type": "image_url",
                "image_url": {"url": f"data:image/jpeg;base64,{state['image_b64']}"},
            },
        ]
    )
    raw = _vision_llm.invoke([msg]).content
    data = _loads_json(raw)
    extracted = ExtractedReceipt(
        merchant_name=data.get("merchant_name"),
        receipt_date=data.get("receipt_date") or None,
        total_amount=data.get("total_amount"),
        currency=data.get("currency") or "INR",
        items=[LineItem(name=i["name"], amount=i["amount"]) for i in data.get("items", [])],
    )
    return {"extracted": extracted}


def _categorize_node(state: GraphState) -> dict:
    extracted = state["extracted"]
    if not extracted.items:
        return {"extracted": extracted}
    prompt = (
        f"Categorize each item into exactly one of: {', '.join(CATEGORIES)}.\n"
        f"Items: {json.dumps([i.name for i in extracted.items])}\n"
        'Return STRICT JSON: {"categories": ["cat1", "cat2", ...]} in the same order. '
        "Return ONLY the JSON."
    )
    raw = _text_llm.invoke([SystemMessage(content=prompt)]).content
    cats = _loads_json(raw).get("categories", [])
    for item, cat in zip(extracted.items, cats):
        item.category = cat if cat in CATEGORIES else "Other"
    return {"extracted": extracted}


def _anomaly_node(state: GraphState) -> dict:
    extracted = state["extracted"]
    avgs = state.get("category_avgs", {})
    flagged: list[str] = []
    for item in extracted.items:
        avg = avgs.get(item.category or "")
        if avg and item.amount > avg * 2:
            flagged.append(f"{item.name} (₹{item.amount:.0f} vs avg ₹{avg:.0f})")
    result = ScanResult(
        **extracted.model_dump(),
        is_anomaly=bool(flagged),
        anomaly_reason="Unusually high: " + "; ".join(flagged) if flagged else None,
    )
    return {"result": result}


def _loads_json(raw: str) -> dict:
    raw = raw.strip()
    if raw.startswith("```"):
        raw = raw.split("```")[1].removeprefix("json").strip()
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        start, end = raw.find("{"), raw.rfind("}")
        return json.loads(raw[start : end + 1]) if start != -1 else {}


def _build_graph():
    g = StateGraph(GraphState)
    g.add_node("extract", _extract_node)
    g.add_node("categorize", _categorize_node)
    g.add_node("anomaly", _anomaly_node)
    g.set_entry_point("extract")
    g.add_edge("extract", "categorize")
    g.add_edge("categorize", "anomaly")
    g.add_edge("anomaly", END)
    return g.compile()


_graph = _build_graph()


def scan_receipt(image_b64: str, category_avgs: dict[str, float]) -> ScanResult:
    out = _graph.invoke({"image_b64": image_b64, "category_avgs": category_avgs})
    return out["result"]
