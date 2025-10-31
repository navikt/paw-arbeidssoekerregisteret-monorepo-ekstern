CREATE TABLE stillinger
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID          NOT NULL,
    adnr                VARCHAR(50),
    tittel              VARCHAR(1000) NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    visning             VARCHAR(20)   NOT NULL,
    kilde               VARCHAR(255)  NOT NULL,
    medium              VARCHAR(255)  NOT NULL,
    referanse           VARCHAR(255)  NOT NULL,
    arbeidsgivernavn    VARCHAR(255),
    stillingstittel     VARCHAR(2000),
    ansettelsesform     VARCHAR(255),
    stillingsprosent    VARCHAR(20),
    stillingsantall     VARCHAR(20),
    sektor              VARCHAR(50),
    soeknadsfrist       VARCHAR(255),
    oppstartsfrist      VARCHAR(255),
    opprettet_timestamp TIMESTAMP(6)  NOT NULL,
    endret_timestamp    TIMESTAMP(6)  NOT NULL,
    publisert_timestamp TIMESTAMP(6)  NOT NULL,
    utloeper_timestamp  TIMESTAMP(6),
    message_timestamp   TIMESTAMP(6)  NOT NULL, -- Tidspunkt melding ble sendt
    inserted_timestamp  TIMESTAMP(6)  NOT NULL, -- Tidspunkt rad ble opprettet
    updated_timestamp   TIMESTAMP(6),           -- Tidspunkt rad ble endret
    UNIQUE (uuid)
);

CREATE TABLE arbeidsgivere
(
    id             BIGSERIAL PRIMARY KEY,
    parent_id      BIGINT      NOT NULL,
    org_form       VARCHAR(20) NOT NULL,
    org_nr         VARCHAR(20),
    parent_org_nr  VARCHAR(20),
    navn           VARCHAR(255),
    offentlig_navn VARCHAR(255),
    FOREIGN KEY (parent_id) REFERENCES stillinger (id) ON DELETE CASCADE
);

CREATE TABLE kategorier
(
    id               BIGSERIAL PRIMARY KEY,
    parent_id        BIGINT       NOT NULL,
    kode             VARCHAR(255) NOT NULL,
    normalisert_kode VARCHAR(255) NOT NULL,
    navn             VARCHAR(255) NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES stillinger (id) ON DELETE CASCADE
);

CREATE TABLE klassifiseringer
(
    id        BIGSERIAL PRIMARY KEY,
    parent_id BIGINT       NOT NULL,
    type      VARCHAR(255) NOT NULL,
    kode      VARCHAR(255) NOT NULL,
    navn      VARCHAR(255) NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES stillinger (id) ON DELETE CASCADE
);

CREATE TABLE lokasjoner
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
    FOREIGN KEY (parent_id) REFERENCES stillinger (id) ON DELETE CASCADE
);

CREATE TABLE egenskaper
(
    id        BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL,
    key       VARCHAR(50),
    value     TEXT,
    FOREIGN KEY (parent_id) REFERENCES stillinger (id) ON DELETE CASCADE
);
