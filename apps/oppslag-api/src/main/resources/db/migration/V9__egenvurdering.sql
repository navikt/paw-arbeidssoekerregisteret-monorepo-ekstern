
CREATE TABLE egenvurdering
(
    id BIGSERIAL PRIMARY KEY,
    egenvurdering_id UUID NOT NULL UNIQUE,
    periode_id UUID NOT NULL,
    opplysninger_om_arbeidssoeker_id UUID NOT NULL,
    profilering_id UUID NOT NULL,
    sendt_inn_av_id BIGINT REFERENCES metadata(id),
    egenvurdering ProfilertTil NOT NULL
);

CREATE INDEX idx_egenvurdering_periode_id ON egenvurdering(periode_id);
CREATE INDEX idx_egenvurdering_id ON egenvurdering(egenvurdering_id);