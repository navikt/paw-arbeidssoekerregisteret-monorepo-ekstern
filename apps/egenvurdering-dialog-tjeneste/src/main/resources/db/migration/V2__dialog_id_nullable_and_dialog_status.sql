ALTER TABLE periode_id_dialog_id
    ALTER COLUMN dialog_id DROP NOT NULL,
    ADD COLUMN dialog_status_code INTEGER,
    ADD COLUMN dialog_error_message TEXT;