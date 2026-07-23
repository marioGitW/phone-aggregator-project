# Phone Aggregator — PRD / Statement of Work

## 1. Overview

Phone Aggregator is a web application that aggregates phone listings from a fixed set of 6 online retailers, matches them against a canonical catalog of known phone models, and presents one unified page per phone showing brand, model, image, specifications, and a list of prices from each retailer — sorted with the lowest price shown first.

**Non-goals for MVP:** no accessories or other product categories, no more than the 6 predefined retailer sites, no user accounts / price alerts / notifications (future phase).

## 2. Tech Stack

- **Scrapers:** Python (Selenium), one module per site, orchestrated by a main crawler
- **Backend:** Spring Boot (Java)
- **Database:** PostgreSQL
- **Frontend:** React

## 3. Core Problem This PRD Solves

Each retailer writes phone titles differently (word order, units, color naming, bundled condition/network info). The system must reliably determine that `"Apple iPhone 15 Pro 256GB Natural Titanium"` and `"iPhone 15 Pro (256 GB) - Natural Titanium"` refer to the same product, while NOT matching `"iPhone 15 Pro Max 256GB"` (different model) or a renewed/refurbished listing (different condition) to the same entry.

## 4. Canonical Catalog

A manually maintained reference table of real phone models/variants — independent of scraping, populated and updated by hand (e.g. via a seed script or admin entry), not derived automatically from scraped titles.

**Product identity granularity:** `brand + model + storage`. Color is NOT part of product identity — it is an attribute on each retailer listing (shown as a selectable variant on the product page). Condition (new/refurbished/renewed) is also a listing attribute, never merged into the same product as a "new" unit.

```
canonical_product
- id
- brand
- model
- storage
- release_year
```

## 5. Matching Pipeline (MVP: rule-based only)

1. Scraper produces a raw listing (title, price, currency, image, url, in_stock, source_site).
2. A normalizer strips retailer boilerplate and standardizes units/tokens (e.g. "256 GB" → "256GB"), extracts brand, model, storage, color, and condition using regex/lookup rules.
3. The normalized (brand, model, storage) key is looked up against `canonical_product`.
4. **Match found:** listing is linked to that canonical product, with color/condition stored on the listing itself.
5. **No match found:** listing is NOT guessed into an existing product. It is sent to a manual review queue instead.

No fuzzy matching or LLM-assisted extraction in MVP — pure deterministic rules against a known, finite catalog.

## 6. Data Model (high level)

```
canonical_product      (id, brand, model, storage, release_year)
retailer_listing       (id, product_id, source_site, color, condition,
                         raw_title, product_url, image_url, last_scraped_at)
price_snapshot         (id, listing_id, price, currency, in_stock, scraped_at)
match_review_queue     (id, raw_title, source_site, raw_price, scraped_at, status)
```

- `price_snapshot` is **append-only** — every crawl run inserts new rows, never overwrites. This preserves full price/stock history over time and leaves room for future visualizations (price trends, availability trends, cross-site comparisons) without a schema change.
- If a listing disappears from a site on a later scrape, it is marked `in_stock = false` on its next snapshot; the record is retained, never deleted.

## 7. Scraper & Crawler Architecture

- Exactly 6 site-specific scrapers, one per retailer, each implementing a common interface (e.g. produces a list of raw listings with the same shape: title, price, currency, image_url, product_url, in_stock) while containing site-specific selector/parsing logic internally.
- A main crawler orchestrates all 6 scrapers on a schedule, currently every 3 days.
- Scrapers behave like a real browsing user (realistic headers/user-agents, delays between requests via Selenium) to avoid being blocked.
- **Failure isolation:** if one site's scraper fails or returns garbage, the crawl run continues for the other 5 sites; the failed site is logged and retried on the next scheduled run (not immediately blocking or failing the whole crawl).
- Out of scope for MVP: adding new sites beyond the 6, scraping non-phone categories.

## 8. Backend Responsibilities (high level, for later detailed design with the agent)

- Expose product listing and product detail endpoints (detail includes specs, image, and all retailer listings sorted by ascending current price).
- Run/trigger the matching pipeline after each crawl (or consume normalized data from the scraper output).
- Provide an admin-facing view/endpoint for the match review queue (approve into an existing product, create a new canonical product, or reject).

## 9. Frontend Responsibilities (high level)

- Product listing page (browse/search phones).
- Product detail page: image (matching selected color), title, specs, list of retailer prices sorted lowest-first, color selector.
- Admin review screen for unmatched listings (later phase, can be minimal for MVP).

## 10. Phased Milestones

1. **Phase 1:** Seed canonical catalog with a small set of models (~10-20). Build 1-2 site scrapers + rule-based matching pipeline end-to-end. Basic product detail page.
2. **Phase 2:** Add remaining scrapers up to 6 sites. Expand catalog. Add review queue admin UI.
3. **Phase 3:** Add price-history visualizations, refine matching rules based on real mismatches found in the review queue.

## 11. Open Assumptions (to confirm with the agent during implementation)

- Currency: assume all 6 sites operate in the same currency/region unless stated otherwise.
- Scheduling mechanism (OS cron vs Spring Boot `@Scheduled` vs Python job scheduler) to be decided during implementation.
- Legal/ToS review per site is the developer's responsibility and out of scope of this document.
