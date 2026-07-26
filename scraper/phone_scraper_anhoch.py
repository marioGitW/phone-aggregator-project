import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from utils.phone_utils import remove_voucher, get_brand_from_raw, format_title, normalize_price

BASE_URL = "https://www.anhoch.com"
CATEGORY_URL = f"{BASE_URL}/categories/mobilni-telefoni/products?brand=&attribute=&toPrice=349980&inStockOnly=2&sort=latest&perPage=30&page="

BRANDS = ["samsung", "apple", "xiaomi", "honor"]

NAME_PREFIXES = [
    "мобилен телефон",
    "паметен телефон",
    "smartphone",
    "mobile phone"
]

class Phone:
    def __init__(self, brand, title, rawTitle, siteLink, price):
        self.brand = brand
        self.title = title
        self.rawTitle = rawTitle
        self.siteLink = siteLink
        self.price = price

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
            EC.presence_of_element_located((By.CSS_SELECTOR, ".product-card"))
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
    return name



def scrape_all_phones():
    driver = get_driver()
    phones = []
    seen_urls = set()
    page = 1

    while True:
        url = f"{CATEGORY_URL}{page}"
        print(f"Scraping page {page} -> {url}")
        driver.get(url)

        loaded = wait_for_cards(driver)
        if not loaded:
            print(f"No cards on page {page}, stopping.")
            break

        cards = driver.find_elements(By.CSS_SELECTOR, ".product-card")

        if not cards:
            print(f"Empty page {page}, stopping.")
            break

        page_urls = []
        for card in cards:
            try:
                href = card.find_element(By.CSS_SELECTOR, "a.product-image").get_attribute("href")
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
                name_element = card.find_element(By.CSS_SELECTOR, "a.product-name")
                raw_name = driver.execute_script("return arguments[0].innerText;", name_element).strip()

                raw_title = remove_voucher(raw_name).lower()

                brand = get_brand_from_raw(raw_title)
                if brand is None or brand not in BRANDS:
                    continue

                title = format_title(clean_name(raw_title), brand)

                try:
                    price_element = card.find_element(By.CSS_SELECTOR, ".product-price")
                    raw_price = driver.execute_script("return arguments[0].innerText;", price_element).strip()
                    price = normalize_price(raw_price)
                except:
                    price = "N/A"

                try:
                    href = card.find_element(By.CSS_SELECTOR, "a.product-image").get_attribute("href")
                    if href and href.startswith("/"):
                        href = BASE_URL + href
                except:
                    href = "N/A"

                if href in seen_urls:
                    continue

                seen_urls.add(href)
                phones.append(Phone(
                    brand=brand,
                    title=title,
                    rawTitle=raw_title,
                    siteLink=(href or "").lower(),
                    price=price,
                ))
                print(f"  + [{brand}] {title}")

            except Exception as e:
                print(f"  Skipped a card: {e}")

        page += 1

    driver.quit()
    return phones


if __name__ == "__main__":
    print("Starting anhoch.com phone scraper...\n")
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