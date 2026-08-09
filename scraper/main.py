import json
import requests

from phone_scraper_ananas import scrape_all_phones as scrape_ananas
from phone_scraper_anhoch import scrape_all_phones as scrape_anhoch
from phone_scraper_ledikom import scrape_ledikom as scrape_ledikom
from phone_scraper_neptun import scrape_all_phones as scrape_neptun
from phone_scraper_setec import scrape_all_phones as scrape_setec
from phone_scraper_tehnomarket import scrape_all_phones as scrape_tehnomarket


OUTPUT_FILE = "phones.json"
BACKEND_URL = "http://localhost:8083/api/phones/import"


def phone_to_dict(phone, source):
    return {
        "brand": phone.brand,
        "title": phone.title,
        "rawTitle": phone.rawTitle,
        "siteLink": phone.siteLink,
        "price": phone.price,
        "imageUrl": getattr(phone, "imageUrl", None),
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
        print(f"ERROR: {filepath} not found.")
        return
    except json.JSONDecodeError:
        print(f"ERROR: {filepath} is not valid JSON.")
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
            print("Backend import failed!")
            print("Status code:", response.status_code)
            print("Response:", response.text)

    except requests.exceptions.ConnectionError:
        print("Could not connect to Spring Boot backend.")
        print("Make sure the backend is running on port 8083.")

    except requests.exceptions.Timeout:
        print("Backend request timed out.")

    except Exception as e:
        print("Unexpected error while sending to backend:", e)


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
            print("Backend import failed!")
            print("Status code:", response.status_code)
            print("Response:", response.text)

    except requests.exceptions.ConnectionError:
        print("Could not connect to Spring Boot backend.")
        print("Make sure the backend is running on port 8083.")

    except requests.exceptions.Timeout:
        print("Backend request timed out.")

    except Exception as e:
        print("Unexpected error while sending to backend:", e)


if __name__ == "__main__":

    all_phones = []

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

        try:
            phones = scrape_fn()

            converted_phones = [
                phone_to_dict(phone, source)
                for phone in phones
            ]

            all_phones.extend(converted_phones)

            print(f"Done! Got {len(phones)} phones from {source}")

        except Exception as e:
            print(f"ERROR scraping {source}: {e}")


    print("\n" + "=" * 50)
    print(f"Total phones scraped: {len(all_phones)}")
    print("=" * 50)


    save_to_json(all_phones, OUTPUT_FILE)


    if all_phones:
        send_to_backend_from_file(OUTPUT_FILE)
    else:
        print("No phones scraped. Skipping backend import.")
