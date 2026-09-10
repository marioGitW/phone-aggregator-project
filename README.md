Phone Aggregator

## Frontend

The frontend (`frontend/`) can run in two modes, chosen by the `VITE_DATA_MODE` build-time
env var (see `frontend/.env.example`):

| Mode | `VITE_DATA_MODE` | Data source | Requires |
|---|---|---|---|
| Live (default) | `api` (or unset) | The Spring Boot backend | Backend running locally (`VITE_API_BASE_URL`, default `http://localhost:8083`) and Postgres populated by the scraper |
| Static demo | `static` | JSON fixtures in `frontend/public/demo-data/` | Nothing - fully standalone, safe to deploy to Vercel with no backend at all |

Both modes implement the exact same data-access interface
(`frontend/src/api/dataClient.js`, backed by `clients/apiDataClient.js` and
`clients/staticDataClient.js`) - search, filters, sorting and pagination behave identically
either way, and no component code differs between them. In static mode, a dismissible "Demo
mode" notice appears on first load (persisted per tab via `sessionStorage`) and a small "Demo"
badge stays pinned in the corner; there's no write-triggering UI in this app to hide.

Run locally against the backend (default):

```
cd frontend
npm install
npm run dev
```

Run locally in static demo mode, no backend needed:

```
cd frontend
VITE_DATA_MODE=static npm run dev
```

### Regenerating the demo fixtures

The fixtures under `frontend/public/demo-data/` are a real, representative subset of the
database (not fabricated data) plus a synthetic 60-day price history per offer (real history is
only ever a day or two deep - see `price-history.json`'s `synthetic`/`note` fields). To
regenerate them from whatever is currently in your local Postgres:

1. Start the backend against a populated database (`cd backend && ./mvnw spring-boot:run`).
2. Call the export endpoint: `curl http://localhost:8083/api/export-demo`.

This overwrites everything under `frontend/public/demo-data/`, including copying any
locally-hosted product images (`/product-images/...`) into `demo-data/images/` and rewriting
their URLs so they work with no backend running. Externally-hosted images (most sources) are
left pointing at the original retailer URL; a handful may not render depending on that site's
hotlinking policy - same as in live mode when the frontend and the image host are different
origins. The selection logic and everything else is in
`backend/.../service/DemoExportService.java`. Pass `?outputDir=/some/path` to write elsewhere.
This endpoint has no auth (matching the rest of this app) - it's a local dev tool, not meant to
be exposed publicly.

### Deploying to Vercel

`frontend/vercel.json` has the SPA rewrite Vercel needs for client-side routing. To deploy:

1. Import the repo into Vercel with **Root Directory** set to `frontend`.
2. Set the environment variable `VITE_DATA_MODE=static` in the Vercel project settings.
3. Deploy. No backend, database, or `VITE_API_BASE_URL` is needed for this mode.

## Scraper

`scraper/main.py` scrapes all six sources (ananas, anhoch, ledikom, neptun,
setec, tehnomarket), writes `scraper/phones.json`, and POSTs the result to
the backend import endpoint. Run it from the `scraper/` directory:

```
py -3 main.py
```

CLI flags:

| Flag | Effect |
|---|---|
| `--limit N` | Only scrape `N` products per source — for testing, not full runs. Skips the `MIN_EXPECTED_PRODUCTS` threshold check for every source, since a limited run is expected to come in under it. |
| `--no-cache` | Bypass ledikom's variant cache (the only scraper that has one) and force a fully fresh scrape — neither reads nor writes the cache file for that run. Use after changing ledikom's extraction logic, so a stale cache entry can't mask the fix. |
| `--skip-backend` | Scrape and write `phones.json`, but don't POST to the backend. |
| `--send-only` | Don't scrape at all — just POST the existing `phones.json` to the backend and exit. Mutually exclusive with `--skip-backend`. Equivalent to the old standalone `send_only.py` (removed). |
| `--verbose` | Show DEBUG-level detail (raw price parsing, individual listings added, rejected extraction candidates) in addition to the default INFO-level per-source progress. |

A full run (no `--limit`) fails loudly instead of importing a bad dataset:
a source returning zero products, or falling below its own
`MIN_EXPECTED_PRODUCTS` threshold, fails the whole run with a non-zero exit
code (after writing whatever was collected, so a failed run still leaves an
artifact to inspect). If every source comes back empty, `phones.json` is
left untouched rather than being truncated.

### Coverage report

`scraper/report_coverage.py` reports, per source and overall, how many
records have `ramGb`/`storageGb`/`colorRaw`/`modelCode` populated in
`phones.json` — alongside what `extract_specs()` would recover from
`rawTitle` independently, so a divergence between "the extractor can find
it" and "the file actually has it" is visible at a glance (this is how the
schema-mismatch and TB-parsing bugs were caught).

```
py -3 report_coverage.py                  # scraper/phones.json
py -3 report_coverage.py path/to/other.json
py -3 report_coverage.py --show-mismatches 10
```

ledikom's specs come from HTML scraping, not title parsing, so a gap there
between "in file" and "extract_specs" is expected, not a bug — the report
notes this inline.
