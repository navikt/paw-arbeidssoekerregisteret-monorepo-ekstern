CREATE TABLE profilering
(
    id        UUID PRIMARY KEY,
    periode_id            UUID        NOT NULL,
    profilering_tidspunkt TIMESTAMP   NOT NULL,
    profilert_til         VARCHAR(30) NOT NULL
);

CREATE TABLE periode
(
    id       UUID PRIMARY KEY,
    identitetsnummer VARCHAR(30) NOT NULL,
    startet          TIMESTAMP   NOT NULL,
    avsluttet        TIMESTAMP
);

CREATE TABLE egenvurdering
(
    id UUID PRIMARY KEY,
    profilering_id   UUID        NOT NULL,
    egenvurdering    VARCHAR(30) NOT NULL,
    CONSTRAINT fk_profilering FOREIGN KEY (profilering_id) REFERENCES profilering (id) ON DELETE CASCADE
);

CREATE TABLE hwm
(
    version         SMALLINT     NOT NULL,
    kafka_topic     VARCHAR(255) NOT NULL,
    kafka_partition SMALLINT     NOT NULL,
    kafka_offset    BIGINT       NOT NULL,
    PRIMARY KEY (version, kafka_topic, kafka_partition)
);