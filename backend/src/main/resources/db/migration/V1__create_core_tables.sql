-- Replaces the single "phones" table with a normalized model: one canonical
-- PhoneModel per device/variant, one Offer per site listing of that model, and an
-- append-only PriceSnapshot history per offer, grouped by the ScrapeRun that observed it.

CREATE TABLE phone_models (
    id            BIGSERIAL PRIMARY KEY,
    brand         VARCHAR(255) NOT NULL,
    model_key     TEXT         NOT NULL,
    storage_gb    INTEGER,
    ram_gb        INTEGER,
    model_code    VARCHAR(100),
    display_name  TEXT         NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);

-- Postgres treats every NULL as distinct in a UNIQUE constraint/index, so a plain
-- UNIQUE(brand, model_key, storage_gb, ram_gb) would let two rows with the same
-- (brand, model_key) and both dimensions unknown duplicate freely. COALESCE both to a
-- sentinel (-1, outside the valid storage/RAM domain - see RAM_VALID/STORAGE_VALID in
-- scraper/utils/spec_extractor.py) so "unknown" compares equal to itself, while any
-- genuinely different known value (8 vs 16 GB RAM) still gets its own row.
CREATE UNIQUE INDEX ux_phone_models_brand_model_key_storage_ram
    ON phone_models (brand, model_key, COALESCE(storage_gb, -1), COALESCE(ram_gb, -1));

CREATE TABLE scrape_runs (
    id             BIGSERIAL PRIMARY KEY,
    started_at     TIMESTAMP    NOT NULL,
    finished_at    TIMESTAMP,
    status         VARCHAR(32)  NOT NULL,
    items_scraped  INTEGER
);

CREATE TABLE offers (
    id                 BIGSERIAL PRIMARY KEY,
    phone_model_id     BIGINT       NOT NULL REFERENCES phone_models(id),
    source             VARCHAR(100) NOT NULL,
    site_link          TEXT         NOT NULL,
    variant_key        VARCHAR(255),
    raw_title          TEXT         NOT NULL,
    color_raw          VARCHAR(255),
    color_canonical    VARCHAR(100),
    image_url          TEXT,
    match_strategy     VARCHAR(32)  NOT NULL,
    match_confidence   DOUBLE PRECISION NOT NULL,
    first_seen_at      TIMESTAMP    NOT NULL,
    last_seen_at       TIMESTAMP    NOT NULL,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX ix_offers_phone_model_id ON offers (phone_model_id);

-- A table-level UNIQUE constraint can only reference columns directly, not an
-- expression like COALESCE(variant_key, site_link) - Postgres requires a unique INDEX
-- on the expression instead. This is the offer's real identity: when a source gives us
-- a stable variant_key, that's the identity; otherwise the site_link is.
CREATE UNIQUE INDEX ux_offers_source_variant_or_link
    ON offers (source, COALESCE(variant_key, site_link));

CREATE TABLE price_snapshots (
    id              BIGSERIAL PRIMARY KEY,
    offer_id        BIGINT    NOT NULL REFERENCES offers(id),
    scrape_run_id   BIGINT    NOT NULL REFERENCES scrape_runs(id),
    price           INTEGER   NOT NULL,
    scraped_at      TIMESTAMP NOT NULL
);

CREATE INDEX ix_price_snapshots_offer_id_scraped_at
    ON price_snapshots (offer_id, scraped_at DESC);

-- Read-only view backing the OfferListing JPA entity: every active offer joined to its
-- phone model and its latest price snapshot, i.e. exactly the rows GET /api/phones,
-- /api/phones/brands, /api/phones/sources and /api/phones/{id}/similar need. Doing the
-- "latest snapshot per offer" join here (DISTINCT ON) means the application can filter,
-- sort and paginate by price the same way it did against the old flat phones table.
CREATE VIEW offer_listings AS
SELECT
    o.id,
    o.phone_model_id,
    pm.brand,
    pm.display_name AS title,
    o.raw_title,
    o.site_link,
    latest.price,
    o.image_url,
    o.source,
    o.first_seen_at AS created_at
FROM offers o
JOIN phone_models pm ON pm.id = o.phone_model_id
JOIN (
    SELECT DISTINCT ON (ps.offer_id) ps.offer_id, ps.price
    FROM price_snapshots ps
    ORDER BY ps.offer_id, ps.scraped_at DESC
) latest ON latest.offer_id = o.id
WHERE o.is_active = TRUE;
