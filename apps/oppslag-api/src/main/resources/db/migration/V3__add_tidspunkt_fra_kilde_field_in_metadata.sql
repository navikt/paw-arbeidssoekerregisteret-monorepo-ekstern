CREATE TYPE AvviksType AS ENUM (
    'UKJENT_VERDI', 'FORSINKELSE', 'RETTING'
    );

CREATE TABLE tidspunkt_fra_kilde
(
    id BIGSERIAL PRIMARY KEY,
    tidspunkt TIMESTAMP(6) NOT NULL,
    avviks_type AvviksType NOT NULL
);

ALTER TABLE metadata ADD COLUMN tidspunkt_fra_kilde_id BIGINT REFERENCES tidspunkt_fra_kilde(id);