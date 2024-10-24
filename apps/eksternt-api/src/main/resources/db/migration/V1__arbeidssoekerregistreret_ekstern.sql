create table periode (
    id BIGSERIAL PRIMARY KEY,
    periode_id UUID NOT NULL UNIQUE,
    identitetsnummer VARCHAR(11) NOT NULL,
    startet TIMESTAMP(6) NOT NULL,
    avsluttet TIMESTAMP(6)
);