-- Removes product fields no longer used by the carton pricing formula.
BEGIN;

ALTER TABLE products DROP COLUMN IF EXISTS flute_type_id;
ALTER TABLE products DROP COLUMN IF EXISTS glue_flap_cm;
ALTER TABLE products DROP COLUMN IF EXISTS trim_cm;

DROP TABLE IF EXISTS product_paper_layers;
DROP TABLE IF EXISTS flute_types;
DROP TABLE IF EXISTS paper_types;
DROP TABLE IF EXISTS pricing_components;

COMMIT;
