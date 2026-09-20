from datetime import date

from pydantic import BaseModel, Field


class LineItem(BaseModel):
    name: str
    amount: float
    category: str | None = None


class ExtractedReceipt(BaseModel):
    merchant_name: str | None = None
    receipt_date: date | None = None
    total_amount: float | None = None
    currency: str = "INR"
    items: list[LineItem] = Field(default_factory=list)


class ScanResult(ExtractedReceipt):
    is_anomaly: bool = False
    anomaly_reason: str | None = None


# --- request bodies ---

class ExpenseIn(BaseModel):
    title: str
    amount: float
    category: str
    note: str | None = None
    expense_date: date | None = None


class SplitMemberIn(BaseModel):
    name: str
    amount_owed: float


class SplitIn(BaseModel):
    title: str
    total_amount: float
    receipt_id: str | None = None
    members: list[SplitMemberIn]
