create table bruker
(
    id                             bigserial primary key,
    identitetsnummer               varchar(11) not null,
    tjenesten_er_aktiv             boolean     not null,
    har_brukt_tjenesten            boolean     not null,
    arbeidssoekerperiode_id        UUID,
    arbeidssoekerperiode_avsluttet timestamp(3),
    unique (identitetsnummer),
    unique (arbeidssoekerperiode_id)
);

create table profilering
(
    id                    bigserial primary key,
    periode_id            UUID         not null,
    profilering_id        UUID         not null,
    profilering_tidspunkt timestamp(3) NOT NULL,
    profilering_resultat  varchar(255) NOT NULL,
    UNIQUE (periode_id),
    UNIQUE (profilering_tidspunkt)
);
