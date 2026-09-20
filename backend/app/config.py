import os
from functools import lru_cache

from dotenv import load_dotenv

load_dotenv()


class Settings:
    SUPABASE_URL: str = os.environ["SUPABASE_URL"]
    SUPABASE_KEY: str = os.environ["SUPABASE_KEY"]
    GROQ_API_KEY: str = os.environ["GROQ_API_KEY"]
    GROQ_VISION_MODEL: str = os.getenv("GROQ_VISION_MODEL", "qwen/qwen3.8-27b")
    GROQ_TEXT_MODEL: str = os.getenv("GROQ_TEXT_MODEL", "openai/gpt-oss-120b")


@lru_cache
def get_settings() -> Settings:
    return Settings()
