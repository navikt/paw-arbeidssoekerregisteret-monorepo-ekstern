package no.naw.paw.minestillinger

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyCount
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.paw.test.data.periode.createProfilering
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.naw.paw.minestillinger.api.ApiStedSoek
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiFylke
import no.naw.paw.minestillinger.api.vo.ApiKommune
import no.naw.paw.minestillinger.api.vo.ApiStillingssoekType
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.harBeskyttetAdresse
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.db.ops.ExposedSøkAdminOps
import no.naw.paw.minestillinger.db.ops.databaseConfigFrom
import no.naw.paw.minestillinger.db.ops.hentBrukerProfilUtenFlagg
import no.naw.paw.minestillinger.db.ops.hentProfileringOrNull
import no.naw.paw.minestillinger.db.ops.lagreProfilering
import no.naw.paw.minestillinger.db.ops.lesFlaggFraDB
import no.naw.paw.minestillinger.db.ops.opprettOgOppdaterBruker
import no.naw.paw.minestillinger.db.ops.postgreSQLContainer
import no.naw.paw.minestillinger.db.ops.skrivFlaggTilDB
import no.naw.paw.minestillinger.db.ops.slettAlleSoekForBruker
import no.naw.paw.minestillinger.domain.TjenesteStatus.INAKTIV
import no.naw.paw.minestillinger.domain.TjenesteStatus.KAN_IKKE_LEVERES
import no.naw.paw.minestillinger.route.BRUKERPROFIL_PATH
import no.naw.paw.minestillinger.route.brukerprofilRoute
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

class Livssyklustest : FreeSpec({
    val currentTime = AtomicReference(Instant.EPOCH)
    val clock = object : Clock {
        override fun now(): Instant = currentTime.get()
    }

    fun forwardTimeByHours(hours: Long) {
        currentTime.updateAndGet { it.plus(hours, ChronoUnit.HOURS) }
    }

    val oauthServer = MockOAuth2Server()
    val postgres = postgreSQLContainer()
    val databaseConfig = databaseConfigFrom(postgres)
    val dataSource = autoClose(initDatabase(databaseConfig))
    beforeSpec {
        oauthServer.start()
        Database.connect(dataSource)
    }
    afterSpec { oauthServer.shutdown() }
    val pdlClient: PdlClient = mockk()
    val brukerprofilTjeneste = BrukerprofilTjeneste(
        pdlClient = pdlClient,
        hentBrukerprofilUtenFlagg = ::hentBrukerProfilUtenFlagg,
        skrivFlagg = ::skrivFlaggTilDB,
        hentFlagg = ::lesFlaggFraDB,
        hentProfilering = ::hentProfileringOrNull,
        slettAlleSøk = ::slettAlleSoekForBruker,
        abTestingRegex = Regex("\\d([02468])\\d{9}"),
        clock = clock
    )
    "Full test av av livessyklus for brukerprofil" - {
        testApplication {
            application {
                configureKtorServer(
                    prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    meterBinders = emptyList(),
                    authProviders = listOf(oauthServer.tokenXAuthProvider)
                )
            }
            val testClient = testClient()
            val olaPeriode = PeriodeFactory.create().build(identitetsnummer = "12111111111")
            val kariPeriode = PeriodeFactory.create().build(identitetsnummer = "14111111111")
            val rolfPeriode = PeriodeFactory.create().build(identitetsnummer = "13111111111")
            currentTime.set(olaPeriode.startet.tidspunkt)
            val olaIdent = Identitetsnummer(olaPeriode.identitetsnummer)
            val kariIdent = Identitetsnummer(kariPeriode.identitetsnummer)
            val rolfIdent = Identitetsnummer(rolfPeriode.identitetsnummer)
            mockkStatic(PdlClient::harBeskyttetAdresse)
            coEvery { pdlClient.harBeskyttetAdresse(Identitetsnummer(olaPeriode.identitetsnummer)) } returns false
            coEvery { pdlClient.harBeskyttetAdresse(Identitetsnummer(kariPeriode.identitetsnummer)) } returns false
            coEvery { pdlClient.harBeskyttetAdresse(Identitetsnummer(rolfPeriode.identitetsnummer)) } returns false

            routing {
                brukerprofilRoute(
                    brukerprofilTjeneste = brukerprofilTjeneste,
                    søkeAdminOps = ExposedSøkAdminOps,
                    clock = clock
                )
            }

            "Rolf som ikke er i testgruppen får tjenestestatus $KAN_IKKE_LEVERES" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                transaction {
                    opprettOgOppdaterBruker(rolfPeriode)
                    lagreProfilering(
                        createProfilering(
                            periodeId = rolfPeriode.id,
                            profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
                        )
                    )
                }
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = rolfIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe rolfIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                    profil.stillingssoek.shouldBeEmpty()
                }
                coVerify(exactly = 0) { pdlClient.harBeskyttetAdresse(Identitetsnummer(rolfPeriode.identitetsnummer)) }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }
            "Når Kari er registert som arbeidssøker antatt gode muligheter kan tjenesten aktiveres" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                transaction {
                    opprettOgOppdaterBruker(kariPeriode)
                    lagreProfilering(
                        createProfilering(
                            periodeId = kariPeriode.id,
                            profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
                        )
                    )
                }
                val aktiverResponse = testClient.put("${BRUKERPROFIL_PATH}/tjenestestatus/AKTIV") {
                    bearerAuth(oauthServer.sluttbrukerToken(id = kariIdent))
                    contentType(Application.Json)
                }
                aktiverResponse.validateAgainstOpenApiSpec()
                withClue(aktiverResponse.bodyAsText()) {
                    aktiverResponse.status shouldBe HttpStatusCode.NoContent
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Kari kan lagre et søk" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                val response = testClient.put("$BRUKERPROFIL_PATH/stillingssoek") {
                    bearerAuth(oauthServer.sluttbrukerToken(id = kariIdent))
                    contentType(Application.Json)
                    setBody(
                        listOf(
                            ApiStedSoek(
                                soekType = ApiStillingssoekType.STED_SOEK_V1,
                                fylker = listOf(
                                    ApiFylke(
                                        navn = "Viken",
                                        fylkesnummer = "30",
                                        kommuner = listOf(
                                            ApiKommune(
                                                navn = "Drammen",
                                                kommunenummer = "3005"
                                            )
                                        )
                                    )
                                ),
                                soekeord = emptyList(),
                                styrk08 = emptyList()
                            )
                        )
                    )
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.NoContent
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Når Ola aldri har vært arbeidssøker får han 404 ved henting av brukerprofil" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.NotFound
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Rett etter at Ola har registrert seg som arbeidssøker finnes profilen med tjenestatestaatus ${KAN_IKKE_LEVERES} (ikke 'antatt gode muligheter' grunnet manglende profilering)" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                transaction {
                    opprettOgOppdaterBruker(olaPeriode)
                }
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe olaIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                    profil.stillingssoek.shouldBeEmpty()
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Etter at Ola er profilert til behov for veiledning er tjenestestatus fremdeles $KAN_IKKE_LEVERES" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                transaction {
                    lagreProfilering(
                        createProfilering(
                            periodeId = olaPeriode.id,
                            profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
                        )
                    )
                }
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe olaIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                    profil.stillingssoek.shouldBeEmpty()
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Når Ola blir profilert til antatt gode muligheter endes tjenestestatus til $INAKTIV" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                transaction {
                    lagreProfilering(
                        createProfilering(periodeId = olaPeriode.id, profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER)
                    )
                }
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe olaIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                    profil.stillingssoek.shouldBeEmpty()
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Etter at Ola har fått gradert adresse" - {
                "er tjenestestatus ${KAN_IKKE_LEVERES}" {
                    LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                    forwardTimeByHours(36)
                    coEvery { pdlClient.harBeskyttetAdresse(olaIdent) } returns true
                    val response = testClient.get(BRUKERPROFIL_PATH) {
                        bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                        contentType(Application.Json)
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                        profil.stillingssoek.shouldBeEmpty()
                    }
                    LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
                }

                "forsøk på å starte tjenesten gir 403 forbidden" {
                    LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                    val response = testClient.put("${BRUKERPROFIL_PATH}/tjenestestatus/AKTIV") {
                        bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                        contentType(Application.Json)
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.Forbidden
                    LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
                }

                "forsøk på å lagre et søk gir 403 forbidden" {
                    LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                    val response = testClient.put("$BRUKERPROFIL_PATH/stillingssoek") {
                        bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                        contentType(Application.Json)
                        setBody(
                            listOf(
                                ApiStedSoek(
                                    soekType = ApiStillingssoekType.STED_SOEK_V1,
                                    fylker = listOf(
                                        ApiFylke(
                                            navn = "Vestland",
                                            fylkesnummer = "46",
                                            kommuner = listOf(
                                                ApiKommune(
                                                    navn = "Bergen",
                                                    kommunenummer = "4601"
                                                )
                                            )
                                        )
                                    ),
                                    soekeord = emptyList(),
                                    styrk08 = emptyList()
                                )
                            )
                        )
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.Forbidden
                    LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
                }
            }
            "Når Ola ikke lenger har gradert adresse" - {
                "er tjenestatus $INAKTIV" {
                    LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                    forwardTimeByHours(36)
                    coEvery { pdlClient.harBeskyttetAdresse(olaIdent) } returns false
                    val response = testClient.get(BRUKERPROFIL_PATH) {
                        bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                        contentType(Application.Json)
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                        profil.stillingssoek.shouldBeEmpty()
                    }
                    LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
                }

                "Tjenesten kan aktiveres igjen" {
                    LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                    val response = testClient.put("${BRUKERPROFIL_PATH}/tjenestestatus/AKTIV") {
                        bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                        contentType(Application.Json)
                    }
                    response.validateAgainstOpenApiSpec()
                    withClue(response.bodyAsText()) {
                        response.status shouldBe HttpStatusCode.NoContent
                    }
                    LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
                }

                "Søk kan lagres" {
                    LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                    val response = testClient.put("$BRUKERPROFIL_PATH/stillingssoek") {
                        bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                        contentType(Application.Json)
                        setBody(
                            listOf(
                                ApiStedSoek(
                                    soekType = ApiStillingssoekType.STED_SOEK_V1,
                                    fylker = listOf(
                                        ApiFylke(
                                            navn = "Vestland",
                                            fylkesnummer = "46",
                                            kommuner = listOf(
                                                ApiKommune(
                                                    navn = "Bergen",
                                                    kommunenummer = "4601"
                                                )
                                            )
                                        )
                                    ),
                                    soekeord = emptyList(),
                                    styrk08 = emptyList()
                                )
                            )
                        )
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.NoContent
                    LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
                }

                "Brukerprofilen skal nå inneholde søket han lagret" {
                    LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                    val response = testClient.get(BRUKERPROFIL_PATH) {
                        bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                        contentType(Application.Json)
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                        profil.stillingssoek.shouldHaveSize(1)
                        profil.stillingssoek.firstOrNull() should { søk ->
                            søk.shouldNotBeNull()
                            søk.shouldBeInstanceOf<ApiStedSoek>()
                            søk.fylker.firstOrNull() should { fylke ->
                                fylke.shouldNotBeNull()
                                fylke.fylkesnummer shouldBe "46"
                                fylke.navn shouldBe "Vestland"
                                fylke.kommuner.firstOrNull() should { kommune ->
                                    kommune.shouldNotBeNull()
                                    kommune.kommunenummer shouldBe "4601"
                                    kommune.navn shouldBe "Bergen"
                                }
                            }
                        }
                    }
                    LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
                }
            }

            "Tjenesten forblir aktiv selv om Ola profileres til antatt behov for veiledning" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                transaction {
                    lagreProfilering(
                        createProfilering(
                            periodeId = olaPeriode.id,
                            profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
                        )
                    )
                }
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe olaIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                    profil.stillingssoek.shouldHaveSize(1)
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Ola kan fremdeles lagre et nytt søk" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                val response = testClient.put("$BRUKERPROFIL_PATH/stillingssoek") {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                    setBody(
                        listOf(
                            ApiStedSoek(
                                soekType = ApiStillingssoekType.STED_SOEK_V1,
                                fylker = listOf(
                                    ApiFylke(
                                        navn = "Vestland",
                                        fylkesnummer = "41",
                                        kommuner = listOf(
                                            ApiKommune(
                                                navn = "Askøy",
                                                kommunenummer = "4102"
                                            )
                                        )
                                    )
                                ),
                                soekeord = emptyList(),
                                styrk08 = emptyList(
                                )
                            )
                        )
                    )
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.NoContent
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Det er det nye søket som er lagret på profilen til Ola" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe olaIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                    profil.stillingssoek.shouldHaveSize(1)
                    profil.stillingssoek.firstOrNull() should { søk ->
                        søk.shouldNotBeNull()
                        søk.shouldBeInstanceOf<ApiStedSoek>()
                        søk.fylker.firstOrNull() should { fylke ->
                            fylke.shouldNotBeNull()
                            fylke.fylkesnummer shouldBe "41"
                            fylke.navn shouldBe "Vestland"
                            fylke.kommuner.firstOrNull() should { kommune ->
                                kommune.shouldNotBeNull()
                                kommune.kommunenummer shouldBe "4102"
                                kommune.navn shouldBe "Askøy"
                            }
                        }
                    }
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Ola kan deaktivere tjenesten" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                val response = testClient.put("${BRUKERPROFIL_PATH}/tjenestestatus/INAKTIV") {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                withClue(response.bodyAsText()) {
                    response.status shouldBe HttpStatusCode.NoContent
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Ola har nå tjenestestatus $INAKTIV selv om han er profilert til antatt behov for veiledning" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Ola kan aktivere tjenesten igjen selv om han er profilert til antatt behov for veiledning" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                val response = testClient.put("${BRUKERPROFIL_PATH}/tjenestestatus/AKTIV") {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                withClue(response.bodyAsText()) {
                    response.status shouldBe HttpStatusCode.NoContent
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Tjenesten er aktiv igjen for Ola" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                    profil.stillingssoek.size shouldBe 1
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Ola får gradert adresse" - {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                forwardTimeByHours(36)
                coEvery { pdlClient.harBeskyttetAdresse(olaIdent) } returns true
                "Tjenestestatus er $KAN_IKKE_LEVERES og søket er slettet" {
                    LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                    val response = testClient.get(BRUKERPROFIL_PATH) {
                        bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                        contentType(Application.Json)
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                        profil.stillingssoek.shouldHaveSize(0)
                    }
                    LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
                }
                "Forsøk på å aktivere tjenesten gir 403 forbidden" {
                    LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                    val aktiverResponse = testClient.put("${BRUKERPROFIL_PATH}/tjenestestatus/AKTIV") {
                        bearerAuth(oauthServer.sluttbrukerToken(id = olaIdent))
                        contentType(Application.Json)
                    }
                    aktiverResponse.validateAgainstOpenApiSpec()
                    aktiverResponse.status shouldBe HttpStatusCode.Forbidden
                    LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }

            "Kari er fremdeles aktiv med 1 søk" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = kariIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe kariIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                    profil.stillingssoek.shouldHaveSize(1)
                    profil.stillingssoek.firstOrNull() should { søk ->
                        søk.shouldNotBeNull()
                        søk.shouldBeInstanceOf<ApiStedSoek>()
                        søk.fylker.firstOrNull() should { fylke ->
                            fylke.shouldNotBeNull()
                            fylke.fylkesnummer shouldBe "30"
                            fylke.navn shouldBe "Viken"
                            fylke.kommuner.firstOrNull() should { kommune ->
                                kommune.shouldNotBeNull()
                                kommune.kommunenummer shouldBe "3005"
                                kommune.navn shouldBe "Drammen"
                            }
                        }
                    }

                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }
            "Nå Kari ikke lenger er arbeidssøker endres tjenestestatus til $INAKTIV, men søket beholdes" {
                LoggerFactory.getLogger("test_logger").info("Starter: ${this.testCase.name.name}")
                transaction {
                    opprettOgOppdaterBruker(
                        PeriodeFactory.create().build(
                            id = kariPeriode.id,
                            identitetsnummer = kariPeriode.identitetsnummer,
                            startet = kariPeriode.startet,
                            avsluttet = MetadataFactory.create().build()
                        )
                    )
                }
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = kariIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe kariIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                    profil.stillingssoek.shouldHaveSize(1)
                }
                LoggerFactory.getLogger("test_logger").info("Avslutter: ${this.testCase.name.name}")
            }
        }
    }
})