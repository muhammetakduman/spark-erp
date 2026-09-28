-- Reusable content ("Şablon"): quote notes/terms, quote item sets (JSON list
-- of product, quantity, unit price) and daily job titles. At most one
-- template per type is the default (used for new quotes' notes).
CREATE TABLE template (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('QUOTE_NOTE', 'QUOTE_ITEM_SET', 'JOB_DESCRIPTION')),
    content TEXT NOT NULL,
    is_default INTEGER NOT NULL DEFAULT 0 CHECK (is_default IN (0, 1)),
    created_at TEXT NOT NULL
);

CREATE UNIQUE INDEX uq_template_type_name ON template (type, name COLLATE NOCASE);

-- The quote notes saved earlier with "Bu metni varsayılan yap" become the
-- default note template, so nothing the user saved is lost.
INSERT INTO template (name, type, content, is_default, created_at)
SELECT 'Varsayılan şartlar', 'QUOTE_NOTE', setting_value, 1, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')
FROM setting
WHERE setting_key = 'quote.defaultNotes' AND setting_value IS NOT NULL AND TRIM(setting_value) <> '';

DELETE FROM setting WHERE setting_key = 'quote.defaultNotes';
