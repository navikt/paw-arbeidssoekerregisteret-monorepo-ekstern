/* ENUMS */
CREATE TYPE BrukerType AS ENUM (
    'UKJENT_VERDI', 'UDEFINERT', 'VEILEDER', 'SYSTEM', 'SLUTTBRUKER'
    );

CREATE TYPE JaNeiVetIkke AS ENUM (
    'JA', 'NEI', 'VET_IKKE'
    );

CREATE TYPE BeskrivelseEnum AS ENUM (
    'UKJENT_VERDI',
    'UDEFINERT',
    'HAR_SAGT_OPP',
    'HAR_BLITT_SAGT_OPP',
    'ER_PERMITTERT',
    'ALDRI_HATT_JOBB',
    'IKKE_VAERT_I_JOBB_SISTE_2_AAR',
    'AKKURAT_FULLFORT_UTDANNING',
    'VIL_BYTTE_JOBB',
    'USIKKER_JOBBSITUASJON',
    'MIDLERTIDIG_JOBB',
    'DELTIDSJOBB_VIL_MER',
    'NY_JOBB',
    'KONKURS',
    'ANNET'
    );

CREATE TYPE ProfilertTil AS ENUM (
    'UKJENT_VERDI',
    'UDEFINERT',
    'ANTATT_GODE_MULIGHETER',
    'ANTATT_BEHOV_FOR_VEILEDNING',
    'OPPGITT_HINDRINGER'
    );

/* Periode */

CREATE TABLE bruker
(
    id BIGSERIAL PRIMARY KEY,
    bruker_id VARCHAR(255) NOT NULL,
    type BrukerType NOT NULL,
    UNIQUE (bruker_id, type)
);

CREATE TABLE metadata
(
    id BIGSERIAL PRIMARY KEY,
    utfoert_av_id BIGINT REFERENCES bruker(id),
    tidspunkt TIMESTAMP(6) NOT NULL,
    kilde VARCHAR(255) NOT NULL,
    aarsak VARCHAR(255) NOT NULL
);

CREATE TABLE periode
(
    id BIGSERIAL PRIMARY KEY,
    periode_id UUID NOT NULL UNIQUE,
    identitetsnummer VARCHAR(11) NOT NULL,
    startet_id BIGINT REFERENCES  metadata(id),
    avsluttet_id BIGINT REFERENCES  metadata(id)
);

/* Opplysninger om arbeidssøker */

CREATE TABLE utdanning
(
    id BIGSERIAL PRIMARY KEY,
    nus VARCHAR(255) NOT NULL,
    bestaatt JaNeiVetIkke,
    godkjent JaNeiVetIkke
);

CREATE TABLE helse
(
    id BIGSERIAL PRIMARY KEY,
    helsetilstand_hindrer_arbeid JaNeiVetIkke NOT NULL
);


CREATE TABLE annet
(
    id BIGSERIAL PRIMARY KEY,
    andre_forhold_hindrer_arbeid JaNeiVetIkke
);

CREATE TABLE opplysninger_om_arbeidssoeker
(
    id BIGSERIAL PRIMARY KEY,
    opplysninger_om_arbeidssoeker_id UUID NOT NULL,
    sendt_inn_av_id BIGINT REFERENCES metadata(id),
    utdanning_id BIGINT REFERENCES utdanning(id),
    helse_id BIGINT REFERENCES helse(id),
    annet_id BIGINT REFERENCES annet(id),
    UNIQUE (opplysninger_om_arbeidssoeker_id)
);

CREATE TABLE periode_opplysninger
(
    id BIGSERIAL PRIMARY KEY,
    periode_id UUID NOT NULL,
    opplysninger_om_arbeidssoeker_id BIGINT REFERENCES opplysninger_om_arbeidssoeker(id),
    UNIQUE (periode_id, opplysninger_om_arbeidssoeker_id)
);

CREATE TABLE beskrivelse_med_detaljer
(
    id BIGSERIAL PRIMARY KEY,
    opplysninger_om_arbeidssoeker_id BIGINT REFERENCES opplysninger_om_arbeidssoeker(id)
);

CREATE TABLE beskrivelse
(
    id BIGSERIAL PRIMARY KEY,
    beskrivelse BeskrivelseEnum NOT NULL,
    beskrivelse_med_detaljer_id BIGINT REFERENCES beskrivelse_med_detaljer(id)
);

CREATE TABLE detaljer
(
    id BIGSERIAL PRIMARY KEY,
    beskrivelse_id BIGINT REFERENCES beskrivelse(id),
    noekkel VARCHAR(50),
    verdi VARCHAR(255)
);

/* Profilering */

CREATE TABLE profilering
(
    id BIGSERIAL PRIMARY KEY,
    profilering_id UUID NOT NULL,
    periode_id UUID,
    opplysninger_om_arbeidssoeker_id UUID,
    sendt_inn_av_id BIGINT REFERENCES metadata(id),
    profilert_til ProfilertTil NOT NULL,
    jobbet_sammenhengende_seks_av_tolv_siste_maneder boolean NOT NULL,
    alder integer NOT NULL
);