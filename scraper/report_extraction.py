"""Report how well extract_specs() does over the real scraped data in phones.json."""
import json
import os

from utils.spec_extractor import extract_specs

PHONES_JSON = os.path.join(os.path.dirname(__file__), "phones.json")


def main():
    with open(PHONES_JSON, encoding="utf-8") as f:
        phones = json.load(f)

    total = len(phones)
    extracted = 0
    ram_and_storage = 0
    storage_only = 0
    failed = []

    per_source = {}

    for phone in phones:
        source = phone.get("source", "unknown")
        stats = per_source.setdefault(source, {"total": 0, "extracted": 0})
        stats["total"] += 1

        specs = extract_specs(phone.get("rawTitle"))
        has_ram = specs["ram_gb"] is not None
        has_storage = specs["storage_gb"] is not None

        if has_storage:
            extracted += 1
            stats["extracted"] += 1
            if has_ram:
                ram_and_storage += 1
            else:
                storage_only += 1
        else:
            failed.append((source, phone.get("rawTitle")))

    print("=== Overall extraction rate ===")
    print(f"{extracted}/{total} ({extracted / total:.1%})")
    print()

    print("=== Per-source extraction rate ===")
    for source in sorted(per_source):
        s = per_source[source]
        print(f"{source:15s} {s['extracted']:4d}/{s['total']:<4d} ({s['extracted'] / s['total']:.1%})")
    print()

    print("=== RAM+storage vs storage-only ===")
    print(f"RAM and storage:  {ram_and_storage}")
    print(f"Storage only:     {storage_only}")
    print(f"Failed (neither): {len(failed)}")
    print()

    print(f"=== Failed extractions ({len(failed)}) ===")
    for source, title in failed:
        print(f"[{source}] {title}")


if __name__ == "__main__":
    main()
