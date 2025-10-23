create table bruker
(
    id                             bigserial primary key,
    identitetsnummer               varchar(11) not null,
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

create table bruker_flagg
(
    id        bigserial primary key,
    bruker_id bigint       not null references bruker (id) on delete cascade,
    navn      varchar(100) not null,
    verdi     boolean      not null,
    tidspunkt timestamp(3) not null,
    unique (bruker_id, navn)
)