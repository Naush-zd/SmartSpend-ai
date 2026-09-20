# SmartSpend AI

AI-powered expense tracker: scan receipts, auto-categorize spending, and split bills. Native Android (Kotlin, Jetpack Compose, MVVM) with a FastAPI + LangGraph + Supabase backend.

## Features

- **AI receipt scan** — snap or pick a receipt photo; a vision LLM extracts the merchant, date, total, and line items, then categorizes each item and flags unusually high spend.
- **Expense tracking** — add and browse manual expenses with categories.
- **Bill splitting** — split a receipt or expense across people and track who has paid.
- **Spending analytics** — spend-by-category summaries.

## Architecture

SmartSpend uses a deliberate hybrid: the app talks to Supabase directly for everything that RLS can protect, and to a small Python backend only for the AI-heavy receipt scan (so the LLM key and orchestration stay server-side).

```
Android app (Kotlin + Jetpack Compose, MVVM)
├── auth + CRUD (expenses, splits) ──────────────►  Supabase (Auth, Postgres, Storage)
└── receipt scan ─────────────►  FastAPI backend  ─►  Groq vision + LangGraph
                                       │
                                       └── writes receipt + line items ─►  Supabase
```

**Why a backend at all?** Auth and CRUD go straight to Supabase (row-level security keeps each user's data isolated). The receipt scan cannot: it needs a Groq API key and a LangGraph pipeline, both of which must stay off the device. So the app sends the image and its Supabase access token to FastAPI, which runs the pipeline and persists the result under the user's identity.

### Scan pipeline (LangGraph)

```
image ─► vision extract (merchant, date, total, items)
      ─► categorize each item
      ─► anomaly check (item > 2× the user's category average)
      ─► save to Supabase (receipts, line_items, image in Storage)
```

## Tech stack

| Layer | Tech |
|---|---|
| Android | Kotlin, Jetpack Compose, MVVM, Navigation, Coil |
| Auth / DB / Storage | Supabase (supabase-kt on Android, supabase-py on the backend) |
| Backend | FastAPI, LangGraph, Groq (vision + text LLMs) |
| Networking | Retrofit + kotlinx-serialization (app → backend), Ktor (supabase-kt) |
| Tooling | uv (Python), Gradle (AGP 9, Kotlin 2.4) |

## Project layout

```
Android/     native Android app (Kotlin, Compose)
backend/     FastAPI + LangGraph service
supabase/    database schema and RLS policies (SQL)
```

## Setup

### Prerequisites

- A Supabase project (run `supabase/schema.sql` in the SQL editor)
- A Groq API key (free at console.groq.com)
- Python 3.12 + uv, Android Studio, an emulator or device

### Backend

```bash
cd backend
cp .env.example .env      # fill in SUPABASE_URL, SUPABASE_KEY, GROQ_API_KEY
uv sync
uv run uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### Android

Add your keys to `Android/local.properties` (gitignored):

```properties
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=<publishable key>
BACKEND_BASE_URL=http://localhost:8000/
```

Then, to let the emulator reach the local backend:

```bash
adb reverse tcp:8000 tcp:8000
```

Build and run from Android Studio (or `./gradlew installDebug`).

## Status

- Backend: receipt scan, expenses, and bill-split endpoints — done.
- Android: authentication and the AI receipt-scan flow — done.
- Planned: expenses and bill-split screens, analytics charts, session persistence, and instrumented tests.
