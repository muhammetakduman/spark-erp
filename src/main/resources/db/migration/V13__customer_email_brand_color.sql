-- Customers get an e-mail address (filled into quotes, and taken over from a
-- quote when its customer is added to the list).
ALTER TABLE customer ADD COLUMN email TEXT NULL;

-- The company's brand colour for PDF headings and table headers.
ALTER TABLE company ADD COLUMN brand_color TEXT NULL;
