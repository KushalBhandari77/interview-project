# AI / LLM Usage

## Tools used

I used **Cursor** (Agent mode) as a supporting tool while building this take-home. It was most helpful for moving faster on repetitive work — project scaffolding, wiring REST endpoints, React layout, test boilerplate, and README setup steps — not for handing off the reconciliation problem itself.

Typical uses:

- breaking down the exercise requirements before I started coding
- generating initial Spring Boot / Vite project structure and Maven wrapper setup
- drafting controllers, DTOs, and UI components I then reviewed and adjusted
- discussing matching edge cases (blank refs, split vs duplicate, orphan refunds)
- unblocking environment issues (JDK path, Spring Initializr version quirks on Windows)
- writing and extending tests when I already knew what behavior I wanted to assert

I read every suggestion before keeping it. The matching engine, fee math, break ordering, and how results should tie to `test/EXPECTED.md` were things I worked through myself and used the datasets to validate — not something I accepted blindly from the assistant.

## Key prompts

Rough order of how I used it:

1. Scaffold a Spring Boot + React repo with the stack the exercise prefers; verify tests and dev server run.
2. Build domain types and fee calculator from `fee_schedule.json` — HALF_UP per fee, then derive expected net.
3. CSV/JSON ingest with quarantine for bad rows; don't let malformed input crash the run.
4. Reconciliation engine: Pass A on ref+sign, fallback for blank refs, then classify breaks in a sensible order.
5. Persistence with Flyway + idempotent re-import; results should survive restart.
6. REST API for import, run, summary, merchant rollup, paginated breaks, break detail.
7. React ops UI — import panel, summary dashboard, expandable breaks with both sides.
8. When tests or UI numbers didn't match EXPECTED.md, ask it to help trace *where* the mismatch was — not to rewrite the whole engine.

For anything that touched matching or fees, I usually described the rule I wanted (or pasted a failing test) rather than asking for a full solution in one shot.

## Where it helped vs. where I steered it

**Helped:** setup speed. Getting Flyway, JPA, a file-backed H2 database, Vite proxy, and a basic test harness in place would have eaten a lot of time manually. Cursor was good at filling in standard Spring/React patterns once I knew the shape I wanted.

**Helped:** UI and API plumbing — DTOs, repository queries, MockMvc tests, dashboard tables. Less thinking about boilerplate, more time on whether the numbers were right.

**I kept the core logic mine:** how tolerance and the T+1..3 window interact with wide-window breaks, duplicate vs split settlement detection, orphan refund as a separate pass, and amount mismatch before fee discrepancy. Those choices came from reading the README and checking against `test/` and `data/`. When the assistant's first classifier ordering or matcher behavior didn't match EXPECTED.md, I fixed the approach and had it help with targeted changes or tests — not a wholesale regen.

**I pushed back on "AI-shaped" code:** I asked for minimal comments, no over-abstraction, and diffs that look like something I'd write myself. I rejected suggestions that added extra layers (extra services, generic frameworks) without a clear payoff for this size of app.

**Debugging:** I used it to suggest hypotheses (wrong fee rounding, ref fallback matching the wrong row, quarantine rows leaking into counts). I still ran `mvnw test`, stepped through failing txn IDs, and treated EXPECTED.md / keystone tests as the source of truth.

## Decisions I made against its suggestions

- **Simplicity over architecture tourism.** Some suggestions split the codebase into more packages or abstractions than I wanted. I kept a clear line: ingest → engine (pure Java) → persistence → API → UI, with tests on the engine and ingest first.

- **Requirements over feature creep.** I prioritized correct reconciliation, persistence, and an ops-usable UI before optional nice-to-haves. Not every idea from the assistant made it in.

- **My matching policy, documented in README.** Tolerance at $0.01, settlement window 1–3 days with wide-window flagged separately, blank-ref fallback on merchant + card + expected net — these were my calls, aligned with the exercise's open questions, even when the assistant proposed alternatives.

- **Stack/version fixes.** Early scaffold hit Spring Boot version / Initializr issues; I corrected those manually rather than accepting whatever template came back first.

- **Verification discipline.** I didn't ship UI numbers I hadn't compared to EXPECTED.md. The assistant helped add money-check and quarantine panels to the dashboard after I decided what belonged on screen vs in tests only.

Overall, Cursor improved my efficiency on scaffolding, wiring, and documentation. The reconciliation rules, validation against the provided datasets, and the tradeoffs I'd defend in a follow-up conversation are mine.
