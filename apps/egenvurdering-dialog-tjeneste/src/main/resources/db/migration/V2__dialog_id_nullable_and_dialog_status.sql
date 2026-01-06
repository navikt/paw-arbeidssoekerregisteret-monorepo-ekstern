ALTER TABLE periode_id_dialog_id
    ALTER COLUMN dialog_id DROP NOT NULL,
    ADD COLUMN dialog_status VARCHAR(255);