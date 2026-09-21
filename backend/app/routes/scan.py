import base64
import uuid

from fastapi import APIRouter, Depends, File, UploadFile

from app.auth.dependencies import CurrentUser, get_current_user, user_scoped_client
from app.chains.receipt_chain import scan_receipt
from app.models.schemas import ScanResult

router = APIRouter(prefix="/scan", tags=["scan"])


def _category_avgs(client, user_id: str) -> dict[str, float]:
    rows = (
        client.table("expenses")
        .select("category, amount")
        .eq("user_id", user_id)
        .execute()
        .data
    )
    totals: dict[str, list[float]] = {}
    for r in rows:
        totals.setdefault(r["category"], []).append(float(r["amount"]))
    return {c: sum(v) / len(v) for c, v in totals.items() if v}


@router.post("", response_model=ScanResult)
async def scan(
    file: UploadFile = File(...),
    user: CurrentUser = Depends(get_current_user),
):
    client = user_scoped_client(user)
    image_bytes = await file.read()
    image_b64 = base64.b64encode(image_bytes).decode()

    result = scan_receipt(image_b64, _category_avgs(client, user.id))

    # upload image to the user's private folder
    path = f"{user.id}/{uuid.uuid4()}.jpg"
    client.storage.from_("receipts").upload(
        path, image_bytes, {"content-type": file.content_type or "image/jpeg"}
    )

    receipt = (
        client.table("receipts")
        .insert(
            {
                "user_id": user.id,
                "image_url": path,
                "merchant_name": result.merchant_name,
                "receipt_date": str(result.receipt_date) if result.receipt_date else None,
                "total_amount": result.total_amount,
                "currency": result.currency,
                "is_anomaly": result.is_anomaly,
                "anomaly_reason": result.anomaly_reason,
            }
        )
        .execute()
        .data[0]
    )

    if result.items:
        client.table("line_items").insert(
            [
                {
                    "receipt_id": receipt["id"],
                    "name": i.name,
                    "amount": i.amount,
                    "category": i.category,
                }
                for i in result.items
            ]
        ).execute()

    result.receipt_id = receipt["id"]
    return result
