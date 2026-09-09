Phone Aggregator

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
