CREATE TABLE periode_id_dialog_id_audit
(
    id                 SERIAL PRIMARY KEY,
    periode_id         UUID      NOT NULL,
    egenvurdering_id   UUID      NOT NULL,
    http_status_code   SMALLINT  NOT NULL,
    error_message      TEXT      NULL,
    inserted_timestamp TIMESTAMP NOT NULL,

    CONSTRAINT fk_periode_id_dialog_id_audit_periode
        FOREIGN KEY (periode_id)
            REFERENCES periode_id_dialog_id (periode_id)
            ON DELETE CASCADE
);

CREATE INDEX periode_id_dialog_id_audit_periode_id_idx
    ON periode_id_dialog_id_audit (periode_id);

ALTER TABLE periode_id_dialog_id
    DROP COLUMN dialog_status_code,
    DROP COLUMN dialog_error_message;
