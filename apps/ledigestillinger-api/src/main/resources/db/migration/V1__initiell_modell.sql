CREATE TABLE stillinger
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL,
    tittel              VARCHAR(255) NOT NULL,
    beskrivelse         VARCHAR(255) NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    kilde               VARCHAR(255) NOT NULL,
    start_date          DATE         NOT NULL,
    annonse_url         VARCHAR(255) NOT NULL,
    publisert_timestamp TIMESTAMP(6) NOT NULL,
    utloeper_timestamp  TIMESTAMP(6),
    endret_timestamp    TIMESTAMP(6) NOT NULL
);

CREATE TABLE metadata
(
    id                 BIGSERIAL PRIMARY KEY,
    parent_id          BIGINT       NOT NULL,
    record_timestamp   TIMESTAMP(6) NOT NULL,
    inserted_timestamp TIMESTAMP(6) NOT NULL,
    updated_timestamp  TIMESTAMP(6),
    FOREIGN KEY (parent_id) REFERENCES stillinger (id)
);

CREATE TABLE arbeidsgivere
(
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT       NOT NULL,
    org_nr      VARCHAR(20)  NOT NULL,
    navn        VARCHAR(255) NOT NULL,
    beskrivelse VARCHAR(255) NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES stillinger (id)
);

CREATE TABLE kategorier
(
    id        BIGSERIAL PRIMARY KEY,
    parent_id BIGINT       NOT NULL,
    type      VARCHAR(10)  NOT NULL,
    kode      VARCHAR(10)  NOT NULL,
    navn      VARCHAR(255) NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES stillinger (id)
);

CREATE TABLE beliggenheter
(
    id        BIGSERIAL PRIMARY KEY,
    parent_id BIGINT       NOT NULL,
    adresse   VARCHAR(255),
    postkode  VARCHAR(255),
    poststed  VARCHAR(255),
    kommune   VARCHAR(255),
    fylke     VARCHAR(255),
    land      VARCHAR(255) NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES stillinger (id)
);
