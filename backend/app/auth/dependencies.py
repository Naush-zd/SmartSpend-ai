from dataclasses import dataclass

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.supabase_client import client_for_user, get_client

_bearer = HTTPBearer()


@dataclass
class CurrentUser:
    id: str
    email: str | None
    access_token: str


async def get_current_user(
    creds: HTTPAuthorizationCredentials = Depends(_bearer),
) -> CurrentUser:
    token = creds.credentials
    try:
        resp = get_client().auth.get_user(token)
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
        )
    if resp is None or resp.user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
        )
    return CurrentUser(id=resp.user.id, email=resp.user.email, access_token=token)


def user_scoped_client(user: CurrentUser):
    return client_for_user(user.access_token)
