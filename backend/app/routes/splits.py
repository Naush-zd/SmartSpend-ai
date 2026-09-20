from fastapi import APIRouter, Depends, HTTPException

from app.auth.dependencies import CurrentUser, get_current_user, user_scoped_client
from app.models.schemas import SplitIn

router = APIRouter(prefix="/splits", tags=["splits"])


@router.get("")
async def list_splits(user: CurrentUser = Depends(get_current_user)):
    client = user_scoped_client(user)
    splits = (
        client.table("splits")
        .select("*, split_members(*)")
        .eq("created_by", user.id)
        .order("created_at", desc=True)
        .execute()
        .data
    )
    return splits


@router.post("")
async def create_split(body: SplitIn, user: CurrentUser = Depends(get_current_user)):
    client = user_scoped_client(user)
    total_owed = sum(m.amount_owed for m in body.members)
    if round(total_owed, 2) > round(body.total_amount, 2) + 0.01:
        raise HTTPException(
            status_code=400,
            detail=f"Members owe ₹{total_owed:.2f} which exceeds total ₹{body.total_amount:.2f}",
        )

    split = (
        client.table("splits")
        .insert(
            {
                "created_by": user.id,
                "receipt_id": body.receipt_id,
                "title": body.title,
                "total_amount": body.total_amount,
            }
        )
        .execute()
        .data[0]
    )

    client.table("split_members").insert(
        [
            {"split_id": split["id"], "name": m.name, "amount_owed": m.amount_owed}
            for m in body.members
        ]
    ).execute()

    return (
        client.table("splits")
        .select("*, split_members(*)")
        .eq("id", split["id"])
        .single()
        .execute()
        .data
    )


@router.patch("/members/{member_id}/paid")
async def mark_paid(member_id: str, user: CurrentUser = Depends(get_current_user)):
    client = user_scoped_client(user)
    updated = (
        client.table("split_members")
        .update({"is_paid": True})
        .eq("id", member_id)
        .execute()
        .data
    )
    if not updated:
        raise HTTPException(status_code=404, detail="Member not found")
    return updated[0]
