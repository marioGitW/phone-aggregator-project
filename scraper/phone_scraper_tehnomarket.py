import re
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from utils.phone_utils import remove_voucher, get_brand_from_raw, format_title, clean_price, extract_image_url

BASE_URL = "https://www.tehnomarket.com.mk"
CATEGORY_URL = f"{BASE_URL}/category/4109/mobilni-telefoni"
IMAGE_SELECTOR = "#category > div > div.box-content > div > div.products.scroll > ul > li:nth-child(1) > div > div > div.span12.text-center > a > div > img"

BRANDS = ["samsung", "apple", "xiaomi", "honor"]

NAME_PREFIXES = [
    "преднарачка -",
    "мобилен телефон",
    "паметен телефон",
    "smartphone",
    "mobile phone"
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
            EC.presence_of_element_located((By.CSS_SELECTOR, "li.product-fix")) # .product-card .col
        )
        time.sleep(1)
        return True
    except:
        return False


def detect_brand_from_raw(raw_title):
    # Kept for backwards compatibility, but use get_brand_from_raw instead
    return get_brand_from_raw(raw_title)


def clean_name(name):
    name = name.lower().strip()
    # remove known prefixes anywhere in the name
    for prefix in NAME_PREFIXES:
        name = name.replace(prefix, "").strip()
    # remove anything inside parentheses including the parentheses themselves
    name = re.sub(r'\(.*?\)', '', name).strip()
    # clean up any double spaces left behind
    name = re.sub(r'\s+', ' ', name).strip()
    return name


def scrape_all_phones():
    driver = get_driver()
    phones = []
    seen_urls = set()
    page = 1

    while True:
        if page == 1:
            url = CATEGORY_URL
        else:
            url = f"{CATEGORY_URL}/page/{page}"
        print(f"Scraping page {page} -> {url}")
        driver.get(url)

        loaded = wait_for_cards(driver)
        if not loaded:
            print(f"No cards on page {page}, stopping.")
            break

        cards = driver.find_elements(By.CSS_SELECTOR, "li.product-fix") # .product-card .col

        if not cards:
            print(f"Empty page {page}, stopping.")
            break

        page_urls = []
        for card in cards:
            try:
                href = card.find_element(By.CSS_SELECTOR, ".pbox-thumbnail > a").get_attribute("href")
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
                raw_name = card.find_element(By.CSS_SELECTOR, ".product-name a").text.strip()
                raw_title = remove_voucher(raw_name).lower()
                name = clean_name(raw_title)
                brand = get_brand_from_raw(raw_title)
                if brand is None or brand not in BRANDS:
                    continue


                try:
                    price_text = card.find_element(By.CSS_SELECTOR, ".nm").text.strip()
                except:
                    price_text = ""

                print(f"[price] raw={price_text!r}")
                price = clean_price(price_text)
                print(f"[price] cleaned={price}")
                if not price_text:
                    print("[price] raw=''" )
                    print("[price] cleaned=0")

                try:
                    href = card.find_element(By.CSS_SELECTOR, ".product-name a").get_attribute("href")
                    if href and href.startswith("/"):
                        href = BASE_URL + href
                except:
                    href = "N/A"

                try:
                    imageUrl = extract_image_url(card, IMAGE_SELECTOR, BASE_URL)
                except:
                    imageUrl = None

                if href in seen_urls:
                    continue

                seen_urls.add(href)
                phones.append(Phone(
                    brand=brand,
                    title=format_title(name, brand),
                    rawTitle=raw_title,
                    siteLink=(href or "").lower(),
                    price=price,
                    imageUrl=imageUrl,
                ))
                print(f"  + [{brand}] {name}")

            except Exception as e:
                print(f"  Skipped a card: {e}")

        page += 1

    driver.quit()
    return phones


if __name__ == "__main__":
    print("Starting tehnomarket.mk phone scraper...\n")
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