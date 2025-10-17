CREATE TABLE stillinger
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL,
    adnr                VARCHAR(50),
    tittel              VARCHAR(255) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    visning             VARCHAR(20)  NOT NULL,
    kilde               VARCHAR(255) NOT NULL,
    medium              VARCHAR(255) NOT NULL,
    referanse           VARCHAR(255) NOT NULL,
    arbeidsgiver_navn   VARCHAR(255),
    opprettet_timestamp TIMESTAMP(6) NOT NULL,
    endret_timestamp    TIMESTAMP(6) NOT NULL,
    publisert_timestamp TIMESTAMP(6) NOT NULL,
    utloeper_timestamp  TIMESTAMP(6),
    message_timestamp   TIMESTAMP(6) NOT NULL, -- Tidspunkt melding ble sendt
    inserted_timestamp  TIMESTAMP(6) NOT NULL, -- Tidspunkt rad ble opprettet
    updated_timestamp   TIMESTAMP(6),          -- Tidspunkt rad ble endret
    UNIQUE (uuid)
);

CREATE TABLE arbeidsgivere
(
    id             BIGSERIAL PRIMARY KEY,
    parent_id      BIGINT      NOT NULL,
    org_form       VARCHAR(20) NOT NULL,
    org_nr         VARCHAR(20) NOT NULL,
    parent_org_nr  VARCHAR(20) NOT NULL,
    navn           VARCHAR(255),
    offentlig_navn VARCHAR(255),
    FOREIGN KEY (parent_id) REFERENCES stillinger (id)
);

CREATE TABLE kategorier
(
    id        BIGSERIAL PRIMARY KEY,
    parent_id BIGINT       NOT NULL,
    kode      VARCHAR(20)  NOT NULL,
    navn      VARCHAR(255) NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES stillinger (id)
);

CREATE TABLE klassifiseringer
(
    id        BIGSERIAL PRIMARY KEY,
    parent_id BIGINT       NOT NULL,
    type      VARCHAR(20)  NOT NULL,
    kode      VARCHAR(20)  NOT NULL,
    navn      VARCHAR(255) NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES stillinger (id)
);

CREATE TABLE beliggenheter
(
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT       NOT NULL,
    adresse     VARCHAR(255),
    postkode    VARCHAR(10),
    poststed    VARCHAR(100),
    kommune     VARCHAR(100),
    kommunekode VARCHAR(20),
    fylke       VARCHAR(100),
    fylkeskode  VARCHAR(20),
    land        VARCHAR(100) NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES stillinger (id)
);

CREATE TABLE egenskaper
(
    id        BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL,
    key       VARCHAR(50),
    value     VARCHAR(255),
    FOREIGN KEY (parent_id) REFERENCES stillinger (id)
);
