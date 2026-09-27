-- Purchase VAT is set independently of sale VAT: a supplier invoice is often
-- "VAT excluded" while the customer is charged "VAT included" (or no VAT).
ALTER TABLE material_item ADD COLUMN purchase_vat_rate INTEGER NULL CHECK (purchase_vat_rate IN (1, 10, 20));
ALTER TABLE material_item ADD COLUMN purchase_vat_included INTEGER NULL CHECK (purchase_vat_included IN (0, 1));

-- Until now a line's single VAT setting applied to its purchase price too;
-- copy it so existing costs and profits stay exactly the same.
UPDATE material_item
SET purchase_vat_rate = vat_rate,
    purchase_vat_included = vat_included
WHERE purchase_unit_price IS NOT NULL AND vat_rate IS NOT NULL;
