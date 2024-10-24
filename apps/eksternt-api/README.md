# paw-arbeidssoekerregisteret-eksternt-api

Eksternt API for å levere arbeidssøkerperioder fra arbeidssøkerregistreret

## Dokumentasjon for API

https://arbeidssoekerperioder.ekstern.dev.nav.no/docs

## Flydiagram

```mermaid
flowchart RL;
    A(Konsumenter);
    subgraph paw-arbeidssoekerregisteret-eksternt-api
        B[Maskinporten];
        id1[REST API <br/> <br/> POST /api/v1/arbeidssoekerperioder <br/> <br/>];
        id2[Helseendepunkter <br/> Opentelemetry tracing <br/> Logging <br/> <br/>];
        C[Services];
        D[(Database)];
        E[Consumer];
    end;
    F[Kafka topic: paw.arbeidssoekerperiode-v1];
    A-->B;
    B-->id1;
    id2~~~id1
    C-->id1;
    D-->C;
    E-->D;
    F-->E;
```

## Teknologier

Øvrige teknologier, rammeverk og biblioteker som er blitt tatt i bruk:

- [**Kotlin**](https://kotlinlang.org/)
- [**Ktor**](https://ktor.io/)
- [**PostgreSQL**](https://www.postgresql.org/)
- [**Flyway**](https://flywaydb.org/)
- [**Gradle**](https://gradle.org/)

## Lokalt oppsett

Under er det satt opp et par ting som må på plass for at applikasjonen og databasen skal fungere.

### JDK 21

JDK 21 må være installert. Enkleste måten å installere riktig versjon av Java er ved å
bruke [sdkman](https://sdkman.io/install).

### Docker

`docker` og `docker-compose` må være installert. For å
installere disse kan du følge oppskriften på [Dockers](https://www.docker.com/) offisielle side. For installering på Mac
trykk [her](https://docs.docker.com/desktop/mac/install/) eller
trykk [her](https://docs.docker.com/engine/install/ubuntu/) for Ubuntu.

Man må også installere `docker-compose` som en separat greie
for [Ubuntu](https://docs.docker.com/compose/install/#install-compose-on-linux-systems). For Mac følger dette med når
man installerer Docker Desktop.

Kjør opp docker containerne med

```sh
docker-compose up -d
```

Se at alle kjører med

```sh
docker ps
```
Fem containere skal kjøre; kakfa, zookeeper, schema-registry, postgres og mock-oauth2-server.

### App

Start app med `./gradlew run` eller start via intellij

### Autentisering

For å kalle APIet lokalt må man være autentisert med et Bearer token.

Vi benytter mock-ouath2-server til å utstede tokens på lokal maskin. Følgende steg kan benyttes til å generere opp et token:

1. Sørg for at containeren for mock-oauth2-server kjører lokalt (docker-compose up -d)
2. Naviger til [mock-oauth2-server sin side for debugging av tokens](http://localhost:8081/default/debugger)
3. Generer et token
4. Trykk på knappen Get a token
5. Skriv inn noe random i "Enter any user/subject" og pid i optional claims, f.eks.

```json
{ "scope": "nav:arbeid:arbeidssokerregisteret.read" }
```

6. Trykk Sign in
7. Kopier verdien for access_token og benytt denne som Bearer i Authorization-header

8. Eksempel:

```sh
$ curl localhost:8080/api/v1/arbeidssoekerperioder -H 'Authorization: Bearer <access_token>'
```

eller benytt en REST-klient (f.eks. [insomnia](https://insomnia.rest/) eller [Postman](https://www.postman.com/product/rest-client/))

## Kafka

Kafka UI ligger i docker-compose, og finnes på http://localhost:9000

### Produser kafkameldinger for lokal utvikling

Kjør `./gradlew produceLocalMessagesForTopics`

Denne tasken sender to meldinger til `arbeidssokerperioder-v1`.

### Consumer

Konsumer meldinger fra `arbeidssokerperioder-v1`

```sh
docker exec -it paw-arbeidssoekerregisteret-eksternt-api_kafka_1 /usr/bin/kafka-console-consumer --bootstrap-server 127.0.0.1:9092 --topic arbeidssoekerperioder-v1
```

## Formatering

Prosjektet bruker kotlinter

Kjør `./gradlew formatKotlin` for autoformatering eller `./gradlew lintKotlin` for å se lint-feil.

Kjør `./gradlew installKotlinterPrePushHook` for å installere pre-push hook som kjører autoformatering før push.

## Deploye kun til dev

Ved å prefikse branch-navn med `dev/`, så vil branchen kun deployes i dev.

```
git checkout -b dev/<navn på branch>
```

evt. rename branch

```
git checkout <opprinnlig-branch>
git branch -m dev/<opprinnlig-branch>
```

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles via issues her på github.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen [#po-arbeid-dev](https://nav-it.slack.com/archives/CCP6QNBSN)

# Lisens

[MIT](LICENSE)
