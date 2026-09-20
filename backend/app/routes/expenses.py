from fastapi import APIRouter, Depends

from app.auth.dependencies import CurrentUser, get_current_user, user_scoped_client
from app.models.schemas import ExpenseIn

router = APIRouter(prefix="/expenses", tags=["expenses"])


@router.get("")
async def list_expenses(user: CurrentUser = Depends(get_current_user)):
    client = user_scoped_client(user)
    return (
        client.table("expenses")
        .select("*")
        .eq("user_id", user.id)
        .order("expense_date", desc=True)
        .execute()
        .data
    )


@router.post("")
async def create_expense(
    body: ExpenseIn, user: CurrentUser = Depends(get_current_user)
):
    client = user_scoped_client(user)
    payload = {
        "user_id": user.id,
        "title": body.title,
        "amount": body.amount,
        "category": body.category,
        "note": body.note,
    }
    if body.expense_date:
        payload["expense_date"] = str(body.expense_date)
    return client.table("expenses").insert(payload).execute().data[0]


@router.delete("/{expense_id}")
async def delete_expense(
    expense_id: str, user: CurrentUser = Depends(get_current_user)
):
    client = user_scoped_client(user)
    client.table("expenses").delete().eq("id", expense_id).execute()
    return {"deleted": expense_id}


@router.get("/summary")
async def spending_summary(user: CurrentUser = Depends(get_current_user)):
    client = user_scoped_client(user)
    return (
        client.table("spending_summary")
        .select("*")
        .eq("user_id", user.id)
        .execute()
        .data
    )
