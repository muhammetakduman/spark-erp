-- A material line's group on its own site ("Diafon", "Pano"). It starts as the
-- product's catalogue category and can be changed per line without touching
-- the catalogue; once saved it is frozen like the line's prices.
ALTER TABLE material_item ADD COLUMN group_name TEXT;

-- Existing lines take their product's category, so old sites are grouped too;
-- lines of products without a category stay NULL ("Kategorisiz").
UPDATE material_item
SET group_name = (SELECT p.category FROM product p WHERE p.id = material_item.product_id)
WHERE group_name IS NULL;

CREATE INDEX idx_material_item_group_name ON material_item(group_name);
