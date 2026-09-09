import argparse
import json
import logging
import sys
import requests

from phone_scraper_ananas import scrape_all_phones as scrape_ananas
from phone_scraper_anhoch import scrape_all_phones as scrape_anhoch
from phone_scraper_ledikom import scrape_ledikom as scrape_ledikom
from phone_scraper_neptun import scrape_all_phones as scrape_neptun
from phone_scraper_setec import scrape_all_phones as scrape_setec
from phone_scraper_tehnomarket import scrape_all_phones as scrape_tehnomarket


OUTPUT_FILE = "phones.json"
BACKEND_URL = "http://localhost:8083/api/phones/import"

logger = logging.getLogger(__name__)


class _ErrorCapture(logging.Handler):
    """Attached to a scraper module's own logger for the duration of its
    scrape call, so main.py can tell whether that source logged an ERROR
    (zero products, under-MIN_EXPECTED_PRODUCTS, etc.) without needing to
    duplicate each scraper's own threshold arithmetic here."""

    def __init__(self):
        super().__init__(level=logging.ERROR)
        self.triggered = False

    def emit(self, record):
        self.triggered = True


def phone_to_dict(phone, source):
    return {
        "brand": phone.brand,
        "title": phone.title,
        "rawTitle": phone.rawTitle,
        "siteLink": phone.siteLink,
        "price": phone.price,
        "imageUrl": getattr(phone, "imageUrl", None),
        "variant_key": getattr(phone, "variant_key", None),
        "ramGb": getattr(phone, "ram_gb", None),
        "storageGb": getattr(phone, "storage_gb", None),
        "colorRaw": getattr(phone, "color_raw", None),
        "modelCode": getattr(phone, "model_code", None),
        "source": source
    }


def save_to_json(phones, filepath):
    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(phones, f, ensure_ascii=False, indent=2)

    print(f"\nSaved {len(phones)} phones to {filepath}")


def send_to_backend_from_file(filepath=OUTPUT_FILE):
    """Read phones from JSON file and send to backend independently of scraping."""
    print(f"\nReading phones from {filepath}...")
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            phones = json.load(f)
        print(f"Loaded {len(phones)} phones from {filepath}")
    except FileNotFoundError:
        logger.error(f"{filepath} not found.")
        return
    except json.JSONDecodeError:
        logger.error(f"{filepath} is not valid JSON.")
        return

    print("Sending phones to Spring Boot backend...")

    try:
        response = requests.post(
            BACKEND_URL,
            json=phones,
            timeout=30
        )

        if response.status_code == 201:
            print("Backend import successful!")
            print(response.json())
        else:
            logger.error(f"Backend import failed! Status code: {response.status_code}")
            logger.error(f"Response: {response.text}")

    except requests.exceptions.ConnectionError:
        logger.error("Could not connect to Spring Boot backend. "
                      "Make sure the backend is running on port 8083.")

    except requests.exceptions.Timeout:
        logger.error("Backend request timed out.")

    except Exception as e:
        logger.error(f"Unexpected error while sending to backend: {e}")


def send_to_backend(phones):
    """Legacy: send phones data from in-memory list (kept for backward compatibility)."""
    print("\nSending phones to Spring Boot backend...")

    try:
        response = requests.post(
            BACKEND_URL,
            json=phones,
            timeout=30
        )

        if response.status_code == 201:
            print("Backend import successful!")
            print(response.json())
        else:
            logger.error(f"Backend import failed! Status code: {response.status_code}")
            logger.error(f"Response: {response.text}")

    except requests.exceptions.ConnectionError:
        logger.error("Could not connect to Spring Boot backend. "
                      "Make sure the backend is running on port 8083.")

    except requests.exceptions.Timeout:
        logger.error("Backend request timed out.")

    except Exception as e:
        logger.error(f"Unexpected error while sending to backend: {e}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Run all phone scrapers")
    parser.add_argument(
        "--limit", type=int, default=None,
        help="Only scrape this many products per source — for testing."
    )
    parser.add_argument(
        "--skip-backend", action="store_true",
        help="Don't POST the scraped results to the backend import endpoint."
    )
    parser.add_argument(
        "--no-cache", action="store_true",
        help="Bypass ledikom's variant cache (the only scraper that has one) "
             "and force a fully fresh scrape."
    )
    parser.add_argument(
        "--verbose", action="store_true",
        help="Show DEBUG-level per-item detail (raw price parsing, individual "
             "listings added, etc.) in addition to the default INFO-level "
             "per-source progress."
    )
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(levelname)s %(name)s: %(message)s",
    )

    all_phones = []
    failed_sources = []

    scrapers = [
        ("ananas", scrape_ananas),
        ("anhoch", scrape_anhoch),
        ("ledikom", scrape_ledikom),
        ("neptun", scrape_neptun),
        ("setec", scrape_setec),
        ("tehnomarket", scrape_tehnomarket),
    ]

    for source, scrape_fn in scrapers:
        print("\n" + "=" * 50)
        print(f"Scraping {source}...")
        print("=" * 50)

        # Watch this source's own logger for the duration of its scrape call:
        # every scraper module logs an ERROR when a full run comes in under
        # its MIN_EXPECTED_PRODUCTS threshold (zero products included, since
        # zero is always under any positive threshold) — catching that here
        # means main.py doesn't need to re-derive each scraper's own
        # zero/under-threshold arithmetic.
        module_logger = logging.getLogger(scrape_fn.__module__)
        error_capture = _ErrorCapture()
        module_logger.addHandler(error_capture)

        try:
            if source == "ledikom":
                phones = scrape_fn(limit=args.limit, use_cache=not args.no_cache)
            else:
                phones = scrape_fn(limit=args.limit)

            converted_phones = [
                phone_to_dict(phone, source)
                for phone in phones
            ]

            all_phones.extend(converted_phones)

            print(f"Done! Got {len(phones)} phones from {source}")

            if error_capture.triggered:
                failed_sources.append(source)

        except Exception as e:
            logger.error(f"scraping {source}: {e}")
            failed_sources.append(source)

        finally:
            module_logger.removeHandler(error_capture)


    print("\n" + "=" * 50)
    print(f"Total phones scraped: {len(all_phones)}")
    print("=" * 50)


    # Never let an empty scrape result truncate an existing good phones.json
    # — a totally empty result (every source failed or errored) means
    # something is badly wrong upstream, and the right response is to leave
    # the last known-good file alone and fail loudly, not overwrite it with
    # nothing.
    if not all_phones:
        logger.error(
            "No phones were scraped from any source — refusing to write "
            f"{OUTPUT_FILE} (would truncate an existing good file)."
        )
        sys.exit(1)

    save_to_json(all_phones, OUTPUT_FILE)


    if args.skip_backend:
        print("Skipping backend import (--skip-backend).")
    elif all_phones:
        send_to_backend_from_file(OUTPUT_FILE)
    else:
        print("No phones scraped. Skipping backend import.")

    if failed_sources:
        logger.error(f"{', '.join(failed_sources)} returned 0 products, fell "
                      "below its MIN_EXPECTED_PRODUCTS threshold, or errored.")
        sys.exit(1)
