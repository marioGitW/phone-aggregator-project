import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from utils.phone_utils import remove_voucher, get_brand_from_raw, format_title, clean_price, extract_image_url

BASE_URL = "https://ananas.mk"
CATEGORY_URL = f"{BASE_URL}/kategorii/telefoni-foto/mobilni-telefoni/pametni-telefoni"
IMAGE_SELECTOR = "#__next > div.sc-1f6z3vw-0.CSEYd > div.sc-1k1vhoz-0.huxjgP > div > div.sc-1iekrn-3.fVCVGx > div.ais-Hits > div > div:nth-child(2) > div > a > div.sc-v9wo15-5.kNjFky > span > img"

BRANDS = ["samsung", "apple", "xiaomi", "honor"]

# words to remove from phone name
NAME_PREFIXES = [
    "мобилен телефон",
    "паметен телефон",
    "mobile phone",
    "smartphone","виолетов","син","црн","sandy","purple","ментол",
    "зелен","полноќно","црно","cool","moonlight","blue","сив","6/128","8/128"
    "black","тиркизна","боја","златно-песок","-","сина","frost","виолетов","8/256",
    "midnight","ocean","жолт"
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
    options.add_argument("--window-size=1920,1080")
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
            EC.presence_of_element_located((By.CSS_SELECTOR, "a[href*='/proizvod/']"))
        )
        time.sleep(2)
        return True
    except:
        return False


def detect_brand_from_raw(raw_title):
    # Kept for backwards compatibility, but use get_brand_from_raw instead
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
        url = f"{CATEGORY_URL}?page={page}"
        print(f"Scraping page {page} -> {url}")
        driver.get(url)

        loaded = wait_for_cards(driver)
        if not loaded:
            print(f"No cards on page {page}, stopping.")
            break

        # scroll to bottom to trigger lazy loading of all cards
        driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
        time.sleep(2)

        cards = driver.find_elements(By.CSS_SELECTOR, "a[href*='/proizvod/']")
        print(f"Found {len(cards)} products on page {page}")

        if not cards:
            break

        # duplicate page check
        page_urls = [c.get_attribute("href") for c in cards]
        new_urls = [u for u in page_urls if u not in seen_urls]
        if not new_urls:
            print(f"Page {page} is a duplicate, stopping.")
            break

        for card in cards:
            try:
                href = card.get_attribute("href") or "N/A"
                if href in seen_urls:
                    continue
                seen_urls.add(href)

                if href.startswith("/"):
                    href = BASE_URL + href

                # raw name for brand detection before cleaning
                try:
                    raw_name = card.find_element(By.CSS_SELECTOR, "h3").text.strip()
                except:
                    raw_name = "N/A"

                # Remove voucher and lowercase -> rawTitle
                raw_title = remove_voucher(raw_name).lower()

                # detect brand from raw title (first word)
                brand = get_brand_from_raw(raw_title)
                if brand is None or brand not in BRANDS:
                    continue

                # clean and apply brand-specific formatting -> title
                title = format_title(clean_name(raw_title), brand)

                # price - normalize to only digits
                try:
                    raw_price = ""
                    price = 0
                    price_spans = card.find_elements(By.CSS_SELECTOR, "span")
                    for span in reversed(price_spans):
                        raw_price = span.text.strip()
                        clean = raw_price.replace(".", "").replace(",", "")
                        if clean.isdigit() and len(clean) >= 3:
                            break
                    print(f"[price] raw={raw_price!r}")
                    price = clean_price(raw_price)
                    print(f"[price] cleaned={price}")
                except:
                    print("[price] raw=''" )
                    print("[price] cleaned=0")
                    price = 0

                try:
                    imageUrl = extract_image_url(card, IMAGE_SELECTOR, BASE_URL)
                except:
                    imageUrl = None

                phones.append(Phone(
                    brand=brand,
                    title=title,
                    rawTitle=raw_title,
                    siteLink=(href or "").lower(),
                    price=price,
                    imageUrl=imageUrl,
                ))
                print(f"  + [{brand}] {title}")

            except Exception as e:
                print(f"  Skipped a card: {e}")

        page += 1

    driver.quit()
    return phones


if __name__ == "__main__":
    print("Starting ananas.mk phone scraper...\n")
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