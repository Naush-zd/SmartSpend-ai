from supabase import Client, create_client

from app.config import get_settings

_settings = get_settings()


def get_client() -> Client:
    """Base client using the publishable key. Respects RLS once a user
    access token is attached via postgrest.auth(token)."""
    return create_client(_settings.SUPABASE_URL, _settings.SUPABASE_KEY)


def client_for_user(access_token: str) -> Client:
    """A client scoped to the logged-in user so every sub-client — database,
    storage, functions — runs under their RLS policies. set_session applies
    the access token across all of them; the refresh token is unused in this
    per-request context, so a placeholder is fine."""
    client = create_client(_settings.SUPABASE_URL, _settings.SUPABASE_KEY)
    client.auth.set_session(access_token, "unused-in-request-context")
    return client
