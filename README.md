# Settlement Reconciliation

Full-stack settlement reconciliation: ingest ledger CSV + processor settlement JSON, match and classify breaks, persist results, and report them in a React ops UI.

**Stack:** Java 21 · Spring Boot 4.1 · React 19 · Vite · H2 (file-backed) · Flyway

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|--------|
| **JDK** | 21 | `java -version` should show 21.x |
| **Node.js** | 18+ (20+ recommended) | Includes npm |
| **Git** | any recent | To clone the repo |

No global Maven install required — the backend ships with `mvnw` / `mvnw.cmd`.

---

## Clone

```bash
git clone <your-fork-url>
cd interview-project
```

---

## Backend

All backend commands run from the `backend/` directory.

### Run tests (recommended first step)

**Windows (PowerShell):**

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"   # adjust if JDK is elsewhere
.\mvnw.cmd test
```

**macOS / Linux:**

```bash
cd backend
./mvnw test
```

Expect all tests to pass (39 tests as of this submission).

### Start the API (port 8080)

**Windows:**

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**

```bash
cd backend
./mvnw spring-boot:run
```

Verify the server is up:

```bash
curl http://localhost:8080/api/health
# {"status":"ok"}
```

The backend must stay running while you use the UI. Sample dataset import reads files from `../test` and `../data` relative to `backend/`, so start it from that directory.

**Persistence:** H2 database file at `backend/data/reconciliation.mv.db` (created on first run). Results survive restarts.

---

## Frontend

From a **second terminal**, in the repo root:

```bash
cd frontend
npm install
npm run dev
```

Open the URL Vite prints (default **http://localhost:5173**). The dev server proxies `/api` to `http://localhost:8080`.

> If port 5173 is busy, Vite picks the next free port — use whatever URL it shows. Keep the backend on 8080.

Production build (optional):

```bash
cd frontend
npm run build
```

---

## Using the app

1. With **backend** and **frontend** both running, open the UI.
2. Click **Load test sample** (small `test/` set) or **Load full data set** (`data/`).
   - Import and reconciliation run in one step.
3. Review **Run summary**, **Money checks**, **Quarantined rows**, **Merchant rollup**, and **Breaks**.
4. Click a break row to expand ledger vs settlement detail and fee deltas.
5. Use **Past run** in the header to switch between previous runs.

### Manual file upload

Upload both files from the same dataset folder:

| File | Path |
|------|------|
| Internal CSV | `test/internal_transactions.csv` or `data/internal_transactions.csv` |
| Settlement JSON | `test/processor_settlement.json` or `data/processor_settlement.json` |

---

## Verify against `test/EXPECTED.md`

After **Load test sample**, check:

**Import**

| Field | Expected |
|-------|----------|
| Ledger rows | 15 |
| Settlement rows | 17 |
| Quarantined | 5 |

**Run summary**

| Field | Expected |
|-------|----------|
| Clean matches | 8 |
| Break categories | 1 each of unmatched ledger/settlement, amount mismatch, fee discrepancy, duplicate, orphan refund, split, wide window |

**Money checks (valid rows only)**

| Check | Expected |
|-------|----------|
| Total gross (valid sales) | $6,804.12 |
| Total refund gross | −$1,557.02 |
| Total settled | $5,161.00 |
| Total fees | $151.74 |

**Quarantined rows:** 3 internal + 2 settlement.

For the full `data/` set, the UI should show 543 ledger rows, 546 settlement rows, 510 clean matches, and 38 breaks. Automated regression: `ReconciliationKeystoneTest` and `ApiEndToEndTest` in the backend test suite.

---

## Project layout

```
backend/          Spring Boot API, reconciliation engine, persistence
frontend/         React ops UI (Vite dev proxy → :8080)
test/             Small dataset + EXPECTED.md answer key
data/             Full realistic dataset
fee_schedule.json Fee schedule (also copied into backend resources)
```

**Backend packages (high level):**

- `ingest/` — CSV/JSON readers, validation, quarantine
- `fee/` — fee schedule + calculator (HALF_UP per fee)
- `engine/` — matching + break classification (pure Java)
- `persistence/` — Flyway schema, JPA entities
- `service/` — import, run, reporting
- `api/` — REST controllers

---

## Design decisions

| Topic | Choice |
|-------|--------|
| **Amount tolerance** | $0.01 (`recon.tolerance` in `application.yml`) |
| **Settlement window** | T+1 through T+3 days; matches outside that range are flagged as **wide window** (still amount/fee correct) |
| **Blank `merchant_ref`** | Pass B fallback: merchant + card type + last4 + sign + expected net within tolerance + day offset |
| **Duplicate vs split** | Duplicate: each settlement row repeats expected net. Split: partial rows **sum** to expected net |
| **Orphan refunds** | Separate pass after matching; overrides a clean match if the refund ref has no sale in the ledger |
| **Re-import** | Idempotent by SHA-256 hash of both input files |

---

## API (summary)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/imports/sample/{test\|data}` | Load sample datasets |
| POST | `/api/imports` | Multipart upload |
| POST | `/api/runs` | Run reconciliation (`{"batchId": N}`) |
| GET | `/api/runs` | Run history |
| GET | `/api/runs/{id}/summary` | Summary + money checks |
| GET | `/api/runs/{id}/merchants` | Merchant rollup |
| GET | `/api/runs/{id}/breaks` | Paginated breaks (filter by outcome, merchant) |
| GET | `/api/runs/{id}/breaks/{outcomeId}` | Break detail |
| GET | `/api/runs/{id}/quarantine` | Quarantined rows |
| GET | `/api/health` | Health check |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `JAVA_HOME` / Java version errors | Point `JAVA_HOME` at JDK 21 before running `mvnw` |
| UI shows **Forbidden** or API errors | Ensure backend is running on 8080; use the Vite dev URL (not `npm run preview` unless you configure a proxy) |
| Sample import fails | Start backend from `backend/` so `../test` and `../data` resolve correctly |
| Port 5173 in use | Use the alternate port Vite prints (e.g. 5174, 5175) |

---

## Original exercise brief

<details>
<summary>Click to expand the original take-home instructions</summary>

Thanks for your interest in the role. This is a take-home exercise meant to be done in **your own environment, on your own schedule** - we've found that gives you the best chance to show how you actually work. Plan for roughly **3–5 hours**. It is not a race, and it is not meant to be gold-plated; we care far more about the quality of what you build than the quantity.

You'll build a small full-stack app. There will be a follow up conversation where you walk us through your code and your decisions, so **be sure the end product is something you can explain**, not just checks every box.

### The Setup

We're a payments company. When we run a customer's card, two systems end up with a record of that same money - and they never agree cleanly:

- **Our internal ledger** - what _our_ system believes happened the moment we captured the payment: the merchant, the card, the **gross** amount.
- **The processor's settlement file** - what the card networks and our processor _actually settled_ a day or two later, and what they'll pay out: a **net** amount, **after** interchange and processor fees are deducted.

Reconciliation is the daily job of matching those two sides against each other and surfacing everything that _doesn't_ line up - money we're owed but never received, amounts that don't match, fees we may have been overcharged, things settled that we have no record of. It's core payments work, and getting it right is the exercise.

### What You'll Build

A web application that ingests both files, reconciles them, and reports the results:

1. **Import** the two provided files (formats differ - see below).
2. **Reconcile** internal transactions against settlement records.
3. **Persist** the results so they survive a restart.
4. **Report** a reconciliation summary and a drill-down list of every break (mismatch).

### The Data

| Directory | Purpose |
| --------- | ------- |
| `test/`   | Small, hand-verifiable. `test/EXPECTED.md` gives the correct counts for **every** category. |
| `data/`   | Larger, realistic set with breaks of every category mixed in. |

Both sets include deliberately malformed rows that must be quarantined, not reconciled.

See the repo files for full CSV/JSON column definitions, fee rules, break categories, and submission instructions.




</details>
