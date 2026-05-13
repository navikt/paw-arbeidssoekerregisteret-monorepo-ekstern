# Copilot Instructions

## Repository purpose

This is the **ekstern** (external) monorepo for PAW arbeidssøkerregisteret — downstream services that consume the `periode`, `opplysninger`, and `profilering` Kafka topics. Services that need direct access to the internal event log belong in `paw-arbeidssoekerregisteret-monorepo-intern`.

## Build, test, and deploy

```bash
# Build and run tests for all modules
./gradlew build

# Build and run tests for a single module
./gradlew :apps:ledigestillinger-api:build

# Run all tests in a module
./gradlew :apps:ledigestillinger-api:test

# Run a single test class
./gradlew :apps:ledigestillinger-api:test --tests 'no.nav.paw.ledigestillinger.route.StillingRoutesTest'

# Run a single test method
./gradlew :apps:ledigestillinger-api:test --tests 'no.nav.paw.ledigestillinger.route.StillingRoutesTest.Skal hente stilling med uuid'

# Check for dependency updates
./gradlew dependencyUpdates

# Build and push Docker image (done by CI, not locally)
./gradlew -Pversion=<version> -Pimage=<image> :apps:<module>:build :apps:<module>:jib
```

Gradle requires a `githubPassword` property (set in `~/.gradle/gradle.properties` or as env var `ORG_GRADLE_PROJECT_githubPassword`) to resolve packages from GitHub Package Registry.

## Monorepo layout

```
domain/   – shared domain models, Avro schemas, generated API types
lib/      – shared infrastructure (Kafka, security, DB, metrics, config, HTTP clients…)
test/     – shared test helpers (test-data-factory)
apps/     – deployable services (one Gradle subproject per app)
buildSrc/ – convention plugins (jib-chainguard.gradle.kts)
```

All subprojects are declared in `settings.gradle.kts`. All dependency versions live in `gradle/libs.versions.toml` (version catalog).

## Architecture

Each **app** is a self-contained Ktor service wired together through an `ApplicationContext` data class. The context creates all dependencies inline (config, datasource, kafka, services) using default parameter values — no DI framework.

```kotlin
data class ApplicationContext(
    val serverConfig: ServerConfig = loadNaisOrLocalConfiguration(SERVER_CONFIG),
    val kafkaFactory: KafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG)),
    val stillingService: StillingService = ...,
    ...
)
```

The `main()` function creates an `ApplicationContext`, starts an embedded Netty server, and calls `Application.module(applicationContext)` where Ktor plugins are installed.

## Configuration

Config is loaded by Hoplite from TOML files via `loadNaisOrLocalConfiguration(CONFIG_KEY)`. Two resource folders exist per app:

- `src/main/resources/nais/` — production/dev config (mounted as Nais config maps)
- `src/main/resources/local/` — local development overrides

Tests override config by placing files under `src/test/resources/local/`.

## Kafka

- Avro schemas for Kafka messages live in `domain/*-avro-schema` modules (generated via the Avro Gradle plugin).
- Apps consume topics using `lib:kafka-hwm` (high-watermark consumer) or `lib:kafka-streams` (Kafka Streams topology).
- Shared topic names and consumer/producer factories come from `lib:kafka` and `lib:topics`.
- Kafka Streams apps are tested with `kafka-streams-test-utils` (topology test driver).

## Database

Apps that persist data use:
- **Exposed** (Kotlin SQL DSL) with `exposed-jdbc` and `exposed-java-time`
- **HikariCP** for connection pooling
- **Flyway** for migrations — SQL files go in `src/main/resources/db/migration/` following the `V{n}__description.sql` naming convention
- **PostgreSQL** in production, **Testcontainers** (`testcontainers-postgresql`) in tests

## Security

Authentication uses `nav-security-token-validation-ktor` for JWT validation. Config is loaded from `security_config.toml`. Tests use `mock-oauth2-server` and call `mockOAuth2Server.issueTokenXToken()` to produce tokens.

## OpenAPI

Apps with a REST API maintain an OpenAPI spec at `src/main/resources/openapi/documentation.yaml`. The `openApiValidate` task runs before compilation — the spec must be valid for the build to succeed. Tests validate responses against the spec using `validateAgainstOpenApiSpec()` (Atlassian swagger-request-validator).

## Testing conventions

- Test framework: **Kotest** (`FreeSpec` style) with JUnit Platform runner
- Tests that need a database spin up a real PostgreSQL via Testcontainers through a shared `TestContext` / `TestDatabase` helper
- A shared `TestContext.buildWithDatabase()` wires up the full app (including mock OAuth2 server) for route-level tests
- `TestData` objects provide canonical test fixtures; tests use `beforeSpec`/`afterSpec` for setup and teardown
- Tests follow the **GIVEN / WHEN / THEN** comment structure

## Containerization and deployment

- All apps use the `jib-chainguard` convention plugin (from `buildSrc/`) for Docker image builds via Jib
- Base image: Chainguard JRE (`chainguardJavaImage` + `jvmMajorVersion` properties), currently JVM 25
- JVM flags: `-XX:ActiveProcessorCount=8 -XX:+UseZGC -XX:+ZGenerational`
- Deployed to NAIS (GCP) using `nais/nais.yaml` in each app directory
- Each app has its own GitHub Actions workflow under `.github/workflows/<app-name>.yaml` that triggers on changes to that app's directory or shared `lib/`, `domain/`, or `gradle/` paths

## Naming and package conventions

- Kotlin package root: `no.nav.paw.<app-specific-package>`
- Module names: kebab-case (e.g., `ledigestillinger-api`, `kafka-hwm`)
- App entrypoint: `Application.kt` with a `main()` function and an `Application.module()` extension
- Source layout per app: `config/`, `context/`, `model/`, `plugin/`, `route/`, `service/`, `util/` packages
- DAO layer: `model/dao/` contains Exposed `Table` objects and row data classes
- Mapper pattern: `DaoMapper.kt`, `DtoMapper.kt`, `RowMapper.kt` for conversions between layers
