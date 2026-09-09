"""
Report field-population stats read straight from a phones.json file — no
call into extract_specs() here, so this validates what's actually in the
JSON output, not what the extractor is capable of producing.
"""
import argparse
import json
import os

FIELDS = ["ramGb", "storageGb", "colorRaw", "modelCode"]

DEFAULT_PATH = os.path.join(os.path.dirname(__file__), "phones.json")


def load_phones(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def pct(n, total):
    return f"{n}/{total} ({n / total:.1%})" if total else f"{n}/0 (n/a)"


def main():
    parser = argparse.ArgumentParser(description="Report field coverage in phones.json")
    parser.add_argument("path", nargs="?", default=DEFAULT_PATH)
    args = parser.parse_args()

    phones = load_phones(args.path)
    total = len(phones)

    print(f"=== Total records: {total} ({args.path}) ===\n")

    print("=== Overall non-null field coverage ===")
    for field in FIELDS:
        n = sum(1 for p in phones if p.get(field) is not None)
        print(f"{field:12s} {pct(n, total)}")
    print()

    print("=== Per-source field coverage ===")
    sources = {}
    for p in phones:
        sources.setdefault(p.get("source", "unknown"), []).append(p)

    for source in sorted(sources):
        rows = sources[source]
        print(f"\n[{source}] total={len(rows)}")
        for field in FIELDS:
            n = sum(1 for p in rows if p.get(field) is not None)
            print(f"  {field:12s} {pct(n, len(rows))}")
    print()

    print("=== Sample records (3 per source) ===")
    for source in sorted(sources):
        rows = sources[source]
        print(f"\n[{source}]")
        for p in rows[:3]:
            print(f"  rawTitle:  {p.get('rawTitle')!r}")
            print(f"    ramGb={p.get('ramGb')}, storageGb={p.get('storageGb')}, "
                  f"colorRaw={p.get('colorRaw')!r}, modelCode={p.get('modelCode')!r}")

    storage_count = sum(1 for p in phones if p.get("storageGb") is not None)
    print(f"\n=== storageGb non-null count for compare-against-report_extraction.py: {storage_count} ===")


if __name__ == "__main__":
    main()
