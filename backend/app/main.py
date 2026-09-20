from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routes import expenses, scan, splits

app = FastAPI(title="SmartSpend AI", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # tighten for production
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(scan.router)
app.include_router(expenses.router)
app.include_router(splits.router)


@app.get("/health")
async def health():
    return {"status": "ok"}
