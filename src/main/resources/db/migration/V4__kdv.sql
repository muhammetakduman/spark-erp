-- Per-line VAT: vat_rate already exists (NULL = no VAT); vat_included says
-- whether the entered prices already contain that VAT (1) or not (0).
ALTER TABLE material_item ADD COLUMN vat_included INTEGER NULL CHECK (vat_included IN (0, 1));

-- Until now a VAT rate was always added on top of the entered price.
UPDATE material_item SET vat_included = 0 WHERE vat_rate IS NOT NULL;

-- Service/labor fees of a service job can carry their own VAT the same way.
ALTER TABLE job ADD COLUMN service_fee_vat_rate INTEGER NULL CHECK (service_fee_vat_rate IN (1, 10, 20));
ALTER TABLE job ADD COLUMN service_fee_vat_included INTEGER NULL CHECK (service_fee_vat_included IN (0, 1));
ALTER TABLE job ADD COLUMN labor_fee_vat_rate INTEGER NULL CHECK (labor_fee_vat_rate IN (1, 10, 20));
ALTER TABLE job ADD COLUMN labor_fee_vat_included INTEGER NULL CHECK (labor_fee_vat_included IN (0, 1));
