-- The product-offers endpoint now groups offers by (model, source) and needs each
-- offer's color to build the "available colors" summary and per-color links, but the
-- offer_listings view (V1) never exposed color_raw/color_canonical even though the
-- underlying offers table has always had them.
--
-- CREATE OR REPLACE VIEW requires every pre-existing output column to keep its name,
-- type and position - only new columns may be appended at the end - so the two color
-- columns go after created_at rather than sitting next to the other offer-level columns
-- they logically belong with.
CREATE OR REPLACE VIEW offer_listings AS
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
    o.first_seen_at AS created_at,
    o.color_raw,
    o.color_canonical
FROM offers o
JOIN phone_models pm ON pm.id = o.phone_model_id
JOIN (
    SELECT DISTINCT ON (ps.offer_id) ps.offer_id, ps.price
    FROM price_snapshots ps
    ORDER BY ps.offer_id, ps.scraped_at DESC
) latest ON latest.offer_id = o.id
WHERE o.is_active = TRUE;
