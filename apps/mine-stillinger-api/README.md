# paw-arbeidssoekerregisteret-api-mine-stillinger

REST API som lar innloggede brukere hente ledige stillinger tilpasset sin lagrede stillingssøkprofil.

## Hva tjenesten gjør

- Henter ledige stillinger basert på brukerens lagrede søkekriterier via `paw-arbeidssoekerregisteret-api-ledigestillinger`
- Lagrer brukerprofiler i PostgreSQL (Exposed ORM, Flyway-migrasjoner)
- Konsumerer Kafka-hendelser for arbeidssøerperioder og §14a-vedtak
- Sjekker adressebeskyttelse via PDL
- Støtter direktemeldte stillinger for brukere med rett tilgang (via pam-dir-api)

## API

Alle brukerendepunkter krever TokenX-token.

| Metode | Endepunkt | Beskrivelse |
|--------|-----------|-------------|
| `GET` | `/api/v1/ledigestillinger` | Hent ledige stillinger basert på brukerens søkeprofil |
| `GET` | `/api/v1/brukerprofil` | Hent brukerprofil |
| `PUT` | `/api/v1/brukerprofil` | Oppdater brukerprofil |
| `GET` | `/api/v1/stillingssok/kodeverk/styrk` | STYRK-kodeverk |
| `GET` | `/api/v1/stillingssok/kodeverk/fylker` | Fylker |
| `GET` | `/api/v1/stillingssok/kodeverk/kommuner` | Kommuner |
| `GET` | `/internal/isAlive` | Helsesjekk |
| `GET` | `/internal/isReady` | Helsesjekk |
| `GET` | `/internal/hasStarted` | Helsesjekk |
| `GET` | `/internal/metrics` | Prometheus-metrikker |

### Paging – `GET /api/v1/ledigestillinger`

| Parameter | Type | Standard | Gyldige verdier | Beskrivelse |
|-----------|------|----------|-----------------|-------------|
| `page` | int | 1 | ≥ 1 | Sidenummer |
| `pageSize` | int | 10 | 1–100 | Antall treff per side |
| `sort` | string | DESC | ASC, DESC | Sorteringsrekkefølge |

### Direktemeldte stillinger

Har brukeren flagget `InkluderDirekteMeldteStillingerFlagtype` aktivt, gjøres to parallelle kall til stillingstjenesten per forespørsel:

1. **Ordinært søk** – bruker alle lagrede søkekriterier (søkeord, fylker, STYRK-koder).
2. **Direktemeldt søk** – bruker kun utvidede STYRK-koder (via `ArbeidsplassenMapper.relaterteStyrkKoder`), uten fylkesfilter og uten søkeord.

Begge kallene bruker samme `page`, `pageSize` og `sort`. Svarene slås sammen:

- `stillinger`: alle treff fra begge søk kombinert
- `paging.pageSize`: summen av `pageSize` fra begge svar
- `paging.hitSize`: summen av `hitSize` fra begge svar
- `paging.page` og `paging.sortOrder`: hentes fra første svar

Ved direktemeldte stillinger kan faktisk antall returnerte treff og total `hitSize` være opptil det dobbelte av det klienten ber om.

## Avhengigheter

| Tjeneste | Formål |
|----------|--------|
| `paw-arbeidssoekerregisteret-api-ledigestillinger` | Stillingssøk |
| `paw-kafka-key-generator` | Kafka-nøkler |
| `pam-dir-api` (teampam) | Tilgangssjekk for direktemeldte stillinger |
| PDL | Adressebeskyttelse |

## Konfigurasjon

| Variabel | Beskrivelse |
|----------|-------------|
| `LEDIGE_STILLINGER_BASE_URL` | URL til stillings-API |
| `LEDIGE_STILLINGER_TARGET` | Token-audience for stillings-API |
| `PDL_URL` | URL til PDL |
| `PDL_SCOPE` | Scope for PDL |
| `PDL_TEMA` | Tema for PDL-oppslag (vanligvis `OPP`) |
| `PAM_TILGANG_URL` | URL til pam-dir-api |
| `PAM_TILGANG_SCOPE` | Scope for pam-dir-api |
| `PAM_TILGANG_FUNKSJONSNIVAA` | `DEAKTIVERT`, `BARE_BRUKERPROFIL` eller `AKTIVT` |
| `KAFKA_KEYS_SCOPE` | Scope for Kafka-nøkkelgenerator |

## Teknisk stack

- Kotlin / Ktor
- PostgreSQL (Exposed + Flyway)
- Kafka (Avro / Confluent)
- TokenX
- OpenTelemetry
