-- Products and customers that cannot be deleted (they are used by material
-- lines, quotes or jobs) can be made inactive instead: they stay in their own
-- list and in old records, but are no longer offered in pickers.
ALTER TABLE product ADD COLUMN active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1));
ALTER TABLE customer ADD COLUMN active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1));
