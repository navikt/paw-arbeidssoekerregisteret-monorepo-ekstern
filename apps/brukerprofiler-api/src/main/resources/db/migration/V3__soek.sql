create table soek
(
    id          bigserial primary key,
    opprettet   TIMESTAMP(3) not null,
    bruker_id   bigint       not null references bruker (id) on delete cascade,
    sist_kjoert TIMESTAMP(3),
    type        varchar(100) not null,
    soek        jsonb        not null
);
