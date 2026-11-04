CREATE INDEX idx_periode_identitetsnummer
    ON periode (identitetsnummer);

CREATE INDEX idx_profilering_periode_id_tidspunkt
    ON profilering (periode_id, profilering_tidspunkt DESC);

CREATE INDEX idx_egenvurdering_profilering_id
    ON egenvurdering (profilering_id);
