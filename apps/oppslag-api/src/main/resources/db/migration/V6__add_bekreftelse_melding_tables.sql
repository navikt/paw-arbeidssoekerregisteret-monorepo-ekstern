
CREATE TABLE bekreftelse_svar
(
    id BIGSERIAL PRIMARY KEY,
    sendt_inn_id BIGINT REFERENCES metadata(id),
    gjelder_fra TIMESTAMP(6) NOT NULL,
    gjelder_til TIMESTAMP(6) NOT NULL,
    har_jobbet_i_denne_perioden BOOLEAN NOT NULL,
    vil_fortsette_som_arbeidssoeker BOOLEAN NOT NULL
);

CREATE TABLE bekreftelse
(
    id BIGSERIAL PRIMARY KEY,
    bekreftelse_id UUID NOT NULL,
    periode_id UUID NOT NULL,
    bekreftelsesloesning VARCHAR(50) NOT NULL,
    svar_id BIGINT REFERENCES bekreftelse_svar(id)
);

CREATE INDEX idx_bekreftelse_periode_id ON bekreftelse(periode_id);
CREATE INDEX idx_bekreftelse_bekreftelse_id ON bekreftelse(bekreftelse_id);