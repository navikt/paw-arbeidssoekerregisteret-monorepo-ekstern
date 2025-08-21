CREATE TABLE periode_id_dialog_id
(
    periode_id UUID PRIMARY KEY,
    dialog_id               BIGINT NOT NULL,
    inserted_timestamp      TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_dialog_id ON periode_id_dialog_id (dialog_id);
