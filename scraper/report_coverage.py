"""
Report ramGb/storageGb/colorRaw/modelCode coverage for a phones.json file,
per-source and overall, alongside what extract_specs() recovers from
rawTitle independently — so a divergence between "the extractor can find
it" and "the file actually has it" is visible at a glance, instead of
needing two separate scripts (this merges the old report_extraction.py and
report_json_fields.py).

ledikom is the one expected exception: its ramGb/storageGb/colorRaw come
from HTML variant-button/spec-block scraping, not title parsing, and its
titles (e.g. "apple iphone 17e") carry no spec info at all — a gap there
between "in file" and "extract_specs" is normal, not a bug.
"""
import argparse
import json
import os

from utils.spec_extractor import extract_specs

# (json field in phones.json, corresponding key in extract_specs()'s result)
FIELDS = [
    ("ramGb", "ram_gb"),
    ("storageGb", "storage_gb"),
    ("colorRaw", "color_raw"),
    ("modelCode", "model_code"),
]

DEFAULT_PATH = os.path.join(os.path.dirname(__file__), "phones.json")


def load_phones(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def pct(n, total):
    return f"{n}/{total} ({n / total:.1%})" if total else f"{n}/0 (n/a)"


def field_stats(rows):
    """{json_field: {"in_file": n, "extract_specs": n, "mismatch": n}} over rows."""
    stats = {jf: {"in_file": 0, "extract_specs": 0, "mismatch": 0} for jf, _ in FIELDS}
    for p in rows:
        specs = extract_specs(p.get("rawTitle"))
        for json_field, specs_field in FIELDS:
            in_file_val = p.get(json_field)
            specs_val = specs[specs_field]
            if in_file_val is not None:
                stats[json_field]["in_file"] += 1
            if specs_val is not None:
                stats[json_field]["extract_specs"] += 1
            if (in_file_val is not None and specs_val is not None
                    and str(in_file_val).lower() != str(specs_val).lower()):
                stats[json_field]["mismatch"] += 1
    return stats


def print_stats(label, rows, note=""):
    total = len(rows)
    stats = field_stats(rows)
    print(f"\n[{label}] total={total}{note}")
    for json_field, _ in FIELDS:
        s = stats[json_field]
        print(f"  {json_field:12s} in_file={pct(s['in_file'], total):<16s} "
              f"extract_specs={pct(s['extract_specs'], total):<16s} "
              f"mismatches={s['mismatch']}")


def print_mismatches(phones, limit):
    print(f"\n=== Example mismatches (file has a value, extract_specs disagrees; up to {limit} each) ===")
    any_shown = False
    for json_field, specs_field in FIELDS:
        shown = 0
        for p in phones:
            if shown >= limit:
                break
            in_file_val = p.get(json_field)
            if in_file_val is None:
                continue
            specs_val = extract_specs(p.get("rawTitle"))[specs_field]
            if specs_val is not None and str(in_file_val).lower() != str(specs_val).lower():
                print(f"  [{p.get('source')}] {json_field}: file={in_file_val!r} "
                      f"extract_specs={specs_val!r}  rawTitle={p.get('rawTitle')!r}")
                shown += 1
                any_shown = True
    if not any_shown:
        print("  (none)")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("path", nargs="?", default=DEFAULT_PATH,
                         help="Path to phones.json (default: scraper/phones.json)")
    parser.add_argument(
        "--show-mismatches", type=int, default=5, metavar="N",
        help="Print up to N example records per field where the file's "
             "value disagrees with extract_specs() (0 to disable)."
    )
    args = parser.parse_args()

    phones = load_phones(args.path)
    print(f"=== Total records: {len(phones)} ({args.path}) ===")

    sources = {}
    for p in phones:
        sources.setdefault(p.get("source", "unknown"), []).append(p)

    print_stats("OVERALL", phones)
    for source in sorted(sources):
        note = ("  (specs come from HTML, not rawTitle — a gap vs. "
                 "extract_specs is expected)" if source == "ledikom" else "")
        print_stats(source, sources[source], note=note)

    if args.show_mismatches:
        print_mismatches(phones, args.show_mismatches)


if __name__ == "__main__":
    main()
