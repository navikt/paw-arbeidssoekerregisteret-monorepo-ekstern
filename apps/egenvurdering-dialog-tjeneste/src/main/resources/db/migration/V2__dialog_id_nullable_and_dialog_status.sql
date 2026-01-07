ALTER TABLE periode_id_dialog_id
    ALTER COLUMN dialog_id DROP NOT NULL,
    ADD COLUMN dialog_status_code VARCHAR(255),
    ADD COLUMN dialog_error_message VARCHAR(255);
