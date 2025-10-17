create table bruker
(
    id                             bigserial primary key,
    identitetsnummer               varchar(11)  not null,
    kan_tilbys_tjenesten           varchar(10)  not null,
    kan_tilbys_tjenesten_timestamp timestamp(3) not null,
    tjenesten_er_aktiv             boolean      not null,
    er_ikke_interessert            boolean      not null,
    har_brukt_tjenesten            boolean      not null,
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
    profilering_tidspunkt timestamp(3) not null,
    profilering_resultat  varchar(255) not null,
    unique (periode_id),
    unique (profilering_tidspunkt)
);
