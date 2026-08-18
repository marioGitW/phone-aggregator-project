# import re
# import time
# from selenium import webdriver
# from selenium.webdriver.chrome.options import Options
# from selenium.webdriver.common.by import By
# from selenium.webdriver.support import expected_conditions as EC
# from selenium.webdriver.support.ui import WebDriverWait
# from utils.phone_utils import remove_voucher, get_brand_from_raw, format_title, clean_price, extract_image_url
#
# BASE_URL = "https://www.neptun.mk"
# CATEGORY_URL = f"{BASE_URL}/mobilni_telefoni.nspx"
# IMAGE_SELECTOR = "#mainContainer > div > div.recommendedProducts > div.products-container.productSpacer.grid > div:nth-child(1) > div.theProduct > div.col-sm-12.p0 > div > div > div > div > img"
#
# BRANDS = ["samsung", "apple", "xiaomi", "honor"]
#
# NAME_PREFIXES = [
#     "преднарачка -",
#     "мобилен телефон",
#     "паметен телефон",
#     "mobile phone",
#     "smartphone"
# ]
#
#
#
#
# class Phone:
#     def __init__(self, brand, title, rawTitle, siteLink, price, imageUrl=None):
#         self.brand = brand
#         self.title = title
#         self.rawTitle = rawTitle
#         self.siteLink = siteLink
#         self.price = price
#         self.imageUrl = imageUrl
#
#     def __repr__(self):
#         return (f"Phone(brand={self.brand}, title={self.title}, rawTitle={self.rawTitle}, "
#                 f"siteLink={self.siteLink}, price={self.price})")
#
#
# def get_driver():
#     options = Options()
#     options.add_argument("--headless")
#     options.add_argument("--no-sandbox")
#     options.add_argument("--disable-dev-shm-usage")
#     options.add_argument(
#         "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
#         "AppleWebKit/537.36 (KHTML, like Gecko) "
#         "Chrome/120.0.0.0 Safari/537.36"
#     )
#     driver = webdriver.Chrome(options=options)
#     return driver
#
#
# def wait_for_cards(driver, timeout=15):
#     try:
#         WebDriverWait(driver, timeout).until(
#             EC.presence_of_element_located((By.CSS_SELECTOR, ".productWrapperInner"))
#         )
#         time.sleep(1)
#         return True
#     except:
#         return False
#
#
# def detect_brand_from_raw(raw_title):
#     return get_brand_from_raw(raw_title)
#
#
# def clean_name(name):
#     name = name.lower().strip()
#     for prefix in NAME_PREFIXES:
#         name = name.replace(prefix, "").strip()
#     name = re.sub(r'\(.*?\)', '', name).strip()
#     name = re.sub(r'\s+', ' ', name).strip()
#     return name
#
#
# def remove_voucher_prefix(text):
#     """Remove 'ваучер -' prefix from text and return cleaned text."""
#     if not text:
#         return text
#     text_lower = text.lower()
#     if text_lower.startswith("ваучер -"):
#         # Remove "ваучер -" and any extra spaces
#         text = text[8:].strip()
#     return text
#
#
# def scrape_all_phones():
#     driver = get_driver()
#     phones = []
#     seen_urls = set()
#     page = 1
#
#     while True:
#         url = f"{CATEGORY_URL}?page={page}"
#         print(f"Scraping page {page} -> {url}")
#         driver.get(url)
#
#         loaded = wait_for_cards(driver)
#         if not loaded:
#             print(f"No cards on page {page}, stopping.")
#             break
#
#         cards = driver.find_elements(By.CSS_SELECTOR, ".productWrapperInner")
#
#         if not cards:
#             print(f"Empty page {page}, stopping.")
#             break
#
#         page_new_count = 0
#
#         page_urls = []
#         for card in cards:
#             try:
#                 href = card.find_element(By.CSS_SELECTOR, "a.theLink").get_attribute("href")
#                 page_urls.append(href)
#             except:
#                 page_urls.append("")
#
#         new_urls = [u for u in page_urls if u not in seen_urls]
#         if not new_urls:
#             print(f"Page {page} is a duplicate, stopping.")
#             break
#
#         print(f"Found {len(cards)} products on page {page}, filtering by brand...")
#
#         for card in cards:
#             try:
#                 raw_name = card.find_element(By.CSS_SELECTOR, "h2.product-list-item__content--title").text.strip()
#
#                 cleaned_after_voucher = remove_voucher_prefix(raw_name).lower()
#
#                 brand = get_brand_from_raw(cleaned_after_voucher)
#                 if brand is None or brand not in BRANDS:
#                     continue
#
#                 raw_title = remove_voucher(raw_name).lower()
#
#                 title = format_title(clean_name(raw_title), brand)
#
#                 try:
#                     price_text = card.find_element(By.CSS_SELECTOR, "span.priceNum").text.strip()
#                     print(f"[price] raw={price_text!r}")
#                     price = clean_price(price_text)
#                     print(f"[price] cleaned={price}")
#                 except:
#                     print("[price] raw=''" )
#                     print("[price] cleaned=0")
#                     price = 0
#                 try:
#                     href = card.find_element(By.CSS_SELECTOR, "a.theLink").get_attribute("href")
#                     if href and href.startswith("/"):
#                         href = BASE_URL + href
#                 except:
#                     href = "N/A"
#
#                 try:
#                     imageUrl = extract_image_url(card, IMAGE_SELECTOR, BASE_URL)
#                 except:
#                     imageUrl = None
#
#                 if href in seen_urls:
#                     continue
#
#                 seen_urls.add(href)
#                 phones.append(Phone(
#                     brand=brand,
#                     title=title,
#                     rawTitle=raw_title,
#                     siteLink=(href or "").lower(),
#                     price=price,
#                     imageUrl=imageUrl,
#                 ))
#                 page_new_count += 1
#                 print(f"  + [{brand}] {title} -> {href}")
#
#             except Exception as e:
#                 print(f"  Skipped a card: {e}")
#
#         if page_new_count == 0:
#             print(f"No new phones on page {page}, stopping.")
#             break
#
#         page += 1
#
#     driver.quit()
#     return phones
#
#
# if __name__ == "__main__":
#     print("Starting neptun.mk phone scraper...\n")
#     phones = scrape_all_phones()
#
#     print(f"\n{'='*50}")
#     print(f"Total phones scraped: {len(phones)}")
#     print(f"{'='*50}")
#     for brand in BRANDS:
#         count = len([p for p in phones if p.brand == brand])
#         print(f"  {brand}: {count} phones")
#     print()
#     for phone in phones:
#         print(phone)

import os
import re
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from utils.phone_utils import (
    remove_voucher,
    get_brand_from_raw,
    format_title,
    clean_price,
    extract_image_url,
    download_image_locally,
)

BASE_URL = "https://www.neptun.mk"
CATEGORY_URL = f"{BASE_URL}/mobilni_telefoni.nspx"
IMAGE_SELECTOR = "img"

# neptun blocks hotlinked images, so we download them locally and serve them
# from our own backend instead of linking directly to neptun's CDN.
IMAGES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "backend", "product-images")
BACKEND_IMAGE_BASE_URL = "http://localhost:8083/images"

BRANDS = ["samsung", "apple", "xiaomi", "honor"]

NAME_PREFIXES = [
    "преднарачка -",
    "мобилен телефон",
    "паметен телефон",
    "mobile phone",
    "smartphone"
]




class Phone:
    def __init__(self, brand, title, rawTitle, siteLink, price, imageUrl=None):
        self.brand = brand
        self.title = title
        self.rawTitle = rawTitle
        self.siteLink = siteLink
        self.price = price
        self.imageUrl = imageUrl

    def __repr__(self):
        return (f"Phone(brand={self.brand}, title={self.title}, rawTitle={self.rawTitle}, "
                f"siteLink={self.siteLink}, price={self.price})")


def get_driver():
    options = Options()
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument(
        "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    )
    driver = webdriver.Chrome(options=options)
    return driver


def wait_for_cards(driver, timeout=15):
    try:
        WebDriverWait(driver, timeout).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, ".productWrapperInner"))
        )
        time.sleep(1)
        return True
    except:
        return False


def detect_brand_from_raw(raw_title):
    return get_brand_from_raw(raw_title)


def clean_name(name):
    name = name.lower().strip()
    for prefix in NAME_PREFIXES:
        name = name.replace(prefix, "").strip()
    name = re.sub(r'\(.*?\)', '', name).strip()
    name = re.sub(r'\s+', ' ', name).strip()
    return name


def remove_voucher_prefix(text):
    """Remove 'ваучер -' prefix from text and return cleaned text."""
    if not text:
        return text
    text_lower = text.lower()
    if text_lower.startswith("ваучер -"):
        # Remove "ваучер -" and any extra spaces
        text = text[8:].strip()
    return text


def scrape_all_phones():
    driver = get_driver()
    phones = []
    seen_urls = set()
    page = 1

    while True:
        url = f"{CATEGORY_URL}?page={page}"
        print(f"Scraping page {page} -> {url}")
        driver.get(url)

        loaded = wait_for_cards(driver)
        if not loaded:
            print(f"No cards on page {page}, stopping.")
            break

        cards = driver.find_elements(By.CSS_SELECTOR, ".productWrapperInner")

        if not cards:
            print(f"Empty page {page}, stopping.")
            break

        page_new_count = 0

        page_urls = []
        for card in cards:
            try:
                href = card.find_element(By.CSS_SELECTOR, "a.theLink").get_attribute("href")
                page_urls.append(href)
            except:
                page_urls.append("")

        new_urls = [u for u in page_urls if u not in seen_urls]
        if not new_urls:
            print(f"Page {page} is a duplicate, stopping.")
            break

        print(f"Found {len(cards)} products on page {page}, filtering by brand...")

        for card in cards:
            try:
                raw_name = card.find_element(By.CSS_SELECTOR, "h2.product-list-item__content--title").text.strip()

                cleaned_after_voucher = remove_voucher_prefix(raw_name).lower()

                brand = get_brand_from_raw(cleaned_after_voucher)
                if brand is None or brand not in BRANDS:
                    continue

                raw_title = remove_voucher(raw_name).lower()

                title = format_title(clean_name(raw_title), brand)

                try:
                    price_text = card.find_element(By.CSS_SELECTOR, "span.priceNum").text.strip()
                    print(f"[price] raw={price_text!r}")
                    price = clean_price(price_text)
                    print(f"[price] cleaned={price}")
                except:
                    print("[price] raw=''" )
                    print("[price] cleaned=0")
                    price = 0
                try:
                    href = card.find_element(By.CSS_SELECTOR, "a.theLink").get_attribute("href")
                    if href and href.startswith("/"):
                        href = BASE_URL + href
                except:
                    href = "N/A"

                try:
                    remote_image_url = extract_image_url(card, IMAGE_SELECTOR, BASE_URL)
                    imageUrl = download_image_locally(
                        remote_image_url, "neptun", BASE_URL, IMAGES_DIR, BACKEND_IMAGE_BASE_URL
                    )
                except:
                    imageUrl = None

                if href in seen_urls:
                    continue

                seen_urls.add(href)
                phones.append(Phone(
                    brand=brand,
                    title=title,
                    rawTitle=raw_title,
                    siteLink=(href or "").lower(),
                    price=price,
                    imageUrl=imageUrl,
                ))
                page_new_count += 1
                print(f"  + [{brand}] {title} -> {href}")

            except Exception as e:
                print(f"  Skipped a card: {e}")

        if page_new_count == 0:
            print(f"No new phones on page {page}, stopping.")
            break

        page += 1

    driver.quit()
    return phones


if __name__ == "__main__":
    print("Starting neptun.mk phone scraper...\n")
    phones = scrape_all_phones()

    print(f"\n{'='*50}")
    print(f"Total phones scraped: {len(phones)}")
    print(f"{'='*50}")
    for brand in BRANDS:
        count = len([p for p in phones if p.brand == brand])
        print(f"  {brand}: {count} phones")
    print()
    for phone in phones:
        print(phone)