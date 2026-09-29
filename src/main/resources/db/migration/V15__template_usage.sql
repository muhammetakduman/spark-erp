-- How often and when a template was last used (quote terms picked, a ready
-- item list added to a quote, a ready job description used for a daily job),
-- shown above the template on the Settings > Templates screen.
ALTER TABLE template ADD COLUMN use_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE template ADD COLUMN last_used_at TEXT NULL;
