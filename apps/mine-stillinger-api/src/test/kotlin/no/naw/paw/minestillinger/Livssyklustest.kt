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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.felles.model.Identitetsnummer
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
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetadresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
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
import no.naw.paw.minestillinger.db.ops.slettHvorPeriodeAvsluttetFør
import no.naw.paw.minestillinger.route.brukerprofilRoute
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
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

    fun createApiStedSoek(
        fylkeNavn: String,
        fylkesnummer: String,
        kommuneNavn: String,
        kommunenummer: String
    ) = ApiStedSoek(
        soekType = ApiStillingssoekType.STED_SOEK_V1,
        fylker = listOf(
            ApiFylke(
                navn = fylkeNavn,
                fylkesnummer = fylkesnummer,
                kommuner = listOf(
                    ApiKommune(
                        navn = kommuneNavn,
                        kommunenummer = kommunenummer
                    )
                )
            )
        ),
        soekeord = emptyList(),
        styrk08 = emptyList()
    )

    fun assertStedSoek(
        søk: ApiStedSoek,
        fylkeNavn: String,
        fylkesnummer: String,
        kommuneNavn: String,
        kommunenummer: String
    ) {
        søk.fylker.firstOrNull() should { fylke ->
            fylke.shouldNotBeNull()
            fylke.fylkesnummer shouldBe fylkesnummer
            fylke.navn shouldBe fylkeNavn
            fylke.kommuner.firstOrNull() should { kommune ->
                kommune.shouldNotBeNull()
                kommune.kommunenummer shouldBe kommunenummer
                kommune.navn shouldBe kommuneNavn
            }
        }
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
            val helper = TestHelper(oauthServer)
            val olaPeriode = PeriodeFactory.create().build(identitetsnummer = "12111111111")
            val olaPeriode2 = PeriodeFactory.create().build(identitetsnummer = olaPeriode.identitetsnummer)
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
                    brukerprofilTjeneste = brukerprofilTjeneste, søkeAdminOps = ExposedSøkAdminOps, clock = clock
                )
            }

            with(helper) {
                "Rolf som ikke er i testgruppen" - {
                "får tjenestestatus ${ApiTjenesteStatus.KAN_IKKE_LEVERES}" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    transaction {
                        opprettOgOppdaterBruker(rolfPeriode)
                        lagreProfilering(
                            createProfilering(
                                periodeId = rolfPeriode.id, profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
                            )
                        )
                    }
                    val response = testClient.getBrukerprofil(rolfIdent)
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe rolfIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                        profil.stillingssoek.shouldBeEmpty()
                    }
                }
                "pdl blir aldri kalt" {
                    coVerify(exactly = 0) { pdlClient.harBeskyttetAdresse(rolfIdent) }
                }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }
            "Når Kari er registert som arbeidssøker antatt gode muligheter kan tjenesten aktiveres" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                transaction {
                    opprettOgOppdaterBruker(kariPeriode)
                    lagreProfilering(
                        createProfilering(
                            periodeId = kariPeriode.id, profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
                        )
                    )
                }
                val aktiverResponse = testClient.setTjenestestatus(ApiTjenesteStatus.AKTIV, kariIdent)
                withClue(aktiverResponse.bodyAsText()) {
                    aktiverResponse.status shouldBe HttpStatusCode.NoContent
                }
                coVerify(exactly = 1) { pdlClient.harBeskyttetAdresse(kariIdent) }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Kari kan lagre et søk" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                val response = testClient.lagreStillingssoek(
                    listOf(createApiStedSoek("Viken", "30", "Drammen", "3005")),
                    kariIdent
                )
                response.status shouldBe HttpStatusCode.NoContent
                coVerify(exactly = 1) { pdlClient.harBeskyttetAdresse(kariIdent) }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Når Ola aldri har vært arbeidssøker får han 404 ved henting av brukerprofil" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                val response = testClient.getBrukerprofil(olaIdent)
                response.status shouldBe HttpStatusCode.NotFound
                coVerify(exactly = 0) { pdlClient.harBeskyttetAdresse(olaIdent) }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Rett etter at Ola har registrert seg som arbeidssøker finnes profilen med tjenestatestaatus ${ApiTjenesteStatus.KAN_IKKE_LEVERES} (ikke 'antatt gode muligheter' grunnet manglende profilering)" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                transaction {
                    opprettOgOppdaterBruker(olaPeriode)
                }
                val response = testClient.getBrukerprofil(olaIdent)
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe olaIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                    profil.stillingssoek.shouldBeEmpty()
                }
                coVerify(exactly = 0) { pdlClient.harBeskyttetAdresse(olaIdent) }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Etter at Ola er profilert til behov for veiledning er tjenestestatus fremdeles ${ApiTjenesteStatus.KAN_IKKE_LEVERES}" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                transaction {
                    lagreProfilering(
                        createProfilering(
                            periodeId = olaPeriode.id, profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
                        )
                    )
                }
                val response = testClient.getBrukerprofil(olaIdent)
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe olaIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                    profil.stillingssoek.shouldBeEmpty()
                }
                coVerify(exactly = 0) { pdlClient.harBeskyttetAdresse(olaIdent) }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Når Ola blir profilert til antatt gode muligheter endes tjenestestatus til ${ApiTjenesteStatus.INAKTIV}" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                transaction {
                    lagreProfilering(
                        createProfilering(periodeId = olaPeriode.id, profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER)
                    )
                }
                val response = testClient.getBrukerprofil(olaIdent)
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe olaIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                    profil.stillingssoek.shouldBeEmpty()
                }
                coVerify(exactly = 0) { pdlClient.harBeskyttetAdresse(olaIdent) }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Etter at Ola har fått gradert adresse" - {
                "er tjenestestatus ${ApiTjenesteStatus.INAKTIV}" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    forwardTimeByHours(36)
                    coEvery { pdlClient.harBeskyttetAdresse(olaIdent) } returns true
                    val response = testClient.getBrukerprofil(olaIdent)
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                        profil.stillingssoek.shouldBeEmpty()
                    }
                    coVerify(exactly = 0) { pdlClient.harBeskyttetAdresse(olaIdent) }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }

                "forsøk på å starte tjenesten gir 403 forbidden" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.setTjenestestatus(ApiTjenesteStatus.AKTIV, olaIdent)
                    response.status shouldBe HttpStatusCode.Forbidden
                    coVerify(exactly = 1) { pdlClient.harBeskyttetAdresse(olaIdent) }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }

                "etter forsøk på å starte skal tjenestestatus være endret til ${ApiTjenesteStatus.KAN_IKKE_LEVERES}" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.getBrukerprofil(olaIdent)
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                        profil.stillingssoek.shouldBeEmpty()
                    }
                    coVerify(exactly = 1) { pdlClient.harBeskyttetAdresse(olaIdent) }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }

                "forsøk på å lagre et søk gir 403 forbidden" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.lagreStillingssoek(
                        listOf(createApiStedSoek("Vestland", "46", "Bergen", "4601")),
                        olaIdent
                    )
                    response.status shouldBe HttpStatusCode.Forbidden
                    coVerify(exactly = 1) { pdlClient.harBeskyttetAdresse(olaIdent) }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }
            }
            "Når Ola ikke lenger har gradert adresse" - {
                "er tjenestestatus fortsatt ${ApiTjenesteStatus.KAN_IKKE_LEVERES}" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    forwardTimeByHours(36)
                    coEvery { pdlClient.harBeskyttetAdresse(olaIdent) } returns false
                    val response = testClient.getBrukerprofil(olaIdent)
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                        profil.stillingssoek.shouldBeEmpty()
                    }
                    coVerify(exactly = 1) { pdlClient.harBeskyttetAdresse(olaIdent) }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }
                "arbeidssøkerperioden avsluttes og det går mer enn X dager" - {
                    "perioden avsluttes" {
                        transaction {
                            opprettOgOppdaterBruker(
                                PeriodeFactory.create().build(
                                    id = olaPeriode.id,
                                    identitetsnummer = olaPeriode.identitetsnummer,
                                    startet = olaPeriode.startet,
                                    avsluttet = MetadataFactory.create().build(
                                        tidspunkt = clock.now() - Duration.ofMinutes(1)
                                    )
                                )
                            )
                        }
                    }
                    "opprydnings script kjører" {
                        slettHvorPeriodeAvsluttetFør(clock.now()) shouldBe 1
                    }
                }
                "ola registrerer seg på nytt som arbeidssøker og blir profilert til antatt gode muligheter" {
                    transaction {
                        opprettOgOppdaterBruker(olaPeriode2)
                        lagreProfilering(
                            createProfilering(
                                periodeId = olaPeriode2.id, profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
                            )
                        )
                    }
                }
                "er tjenestatus ${ApiTjenesteStatus.INAKTIV}" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    forwardTimeByHours(36)
                    coEvery { pdlClient.harBeskyttetAdresse(olaIdent) } returns false
                    val response = testClient.getBrukerprofil(olaIdent)
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                        profil.stillingssoek.shouldBeEmpty()
                    }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }

                "Tjenesten kan aktiveres igjen" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.setTjenestestatus(ApiTjenesteStatus.AKTIV, olaIdent)
                    withClue(response.bodyAsText()) {
                        response.status shouldBe HttpStatusCode.NoContent
                    }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }

                "Søk kan lagres" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.lagreStillingssoek(
                        listOf(createApiStedSoek("Vestland", "46", "Bergen", "4601")),
                        olaIdent
                    )
                    response.status shouldBe HttpStatusCode.NoContent
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }

                "Brukerprofilen skal nå inneholde søket han lagret" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.getBrukerprofil(olaIdent)
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                        profil.stillingssoek.shouldHaveSize(1)
                        profil.stillingssoek.firstOrNull() should { søk ->
                            søk.shouldNotBeNull()
                            søk.shouldBeInstanceOf<ApiStedSoek>()
                            assertStedSoek(søk, "Vestland", "46", "Bergen", "4601")
                        }
                    }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }
            }

            "Tjenesten forblir aktiv selv om Ola profileres til antatt behov for veiledning" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                transaction {
                    lagreProfilering(
                        createProfilering(
                            periodeId = olaPeriode2.id, profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
                        )
                    )
                }
                val response = testClient.getBrukerprofil(olaIdent)
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe olaIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                    profil.stillingssoek.shouldHaveSize(1)
                }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Ola kan fremdeles lagre et nytt søk" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                val response = testClient.lagreStillingssoek(
                    listOf(createApiStedSoek("Vestland", "41", "Askøy", "4102")),
                    olaIdent
                )
                response.status shouldBe HttpStatusCode.NoContent
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Det er det nye søket som er lagret på profilen til Ola" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                val response = testClient.getBrukerprofil(olaIdent)
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe olaIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                    profil.stillingssoek.shouldHaveSize(1)
                    profil.stillingssoek.firstOrNull() should { søk ->
                        søk.shouldNotBeNull()
                        søk.shouldBeInstanceOf<ApiStedSoek>()
                        assertStedSoek(søk, "Vestland", "41", "Askøy", "4102")
                    }
                }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Ola kan deaktivere tjenesten" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                val response = testClient.setTjenestestatus(ApiTjenesteStatus.INAKTIV, olaIdent)
                withClue(response.bodyAsText()) {
                    response.status shouldBe HttpStatusCode.NoContent
                }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Ola har nå tjenestestatus ${ApiTjenesteStatus.INAKTIV} selv om han er profilert til antatt behov for veiledning" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                val response = testClient.getBrukerprofil(olaIdent)
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Ola kan aktivere tjenesten igjen selv om han er profilert til antatt behov for veiledning" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                val response = testClient.setTjenestestatus(ApiTjenesteStatus.AKTIV, olaIdent)
                withClue(response.bodyAsText()) {
                    response.status shouldBe HttpStatusCode.NoContent
                }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Tjenesten er aktiv igjen for Ola" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                val response = testClient.getBrukerprofil(olaIdent)
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                    profil.stillingssoek.size shouldBe 1
                }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Ola får gradert adresse" - {
                testLogger.info("Starter: ${this.testCase.name.name}")
                forwardTimeByHours(36)
                coEvery { pdlClient.harBeskyttetAdresse(olaIdent) } returns true
                "Tjenestestatus er ${ApiTjenesteStatus.AKTIV}" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.getBrukerprofil(olaIdent)
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                        profil.stillingssoek.shouldHaveSize(1)
                    }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }
                "Forsøk på å aktivere tjenesten gir 403 forbidden" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val aktiverResponse = testClient.setTjenestestatus(ApiTjenesteStatus.AKTIV, olaIdent)
                    aktiverResponse.status shouldBe HttpStatusCode.Forbidden
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }
                "Ola sin profil er nå oppdatert i db" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    transaction {
                        hentBrukerProfilUtenFlagg(olaIdent).let { it?.id!! }
                            .let { id -> lesFlaggFraDB(id) } should { flagg ->
                            withClue(flagg.toString()) {
                                flagg.filterIsInstance<HarBeskyttetadresseFlagg>().first().verdi shouldBe true
                                flagg.filterIsInstance<TjenestenErAktivFlagg>().first().verdi shouldBe false
                            }
                        }
                    }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }
                "Tjenestestatus er endret til ${ApiTjenesteStatus.KAN_IKKE_LEVERES} og søket er slettet" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.getBrukerprofil(olaIdent)
                    response.status shouldBe HttpStatusCode.OK
                    response.body<ApiBrukerprofil>() should { profil ->
                        profil.identitetsnummer shouldBe olaIdent.verdi
                        profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                        profil.stillingssoek.shouldHaveSize(0)
                    }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }

            "Kari er fremdeles aktiv med 1 søk" {
                testLogger.info("Starter: ${this.testCase.name.name}")
                val response = testClient.getBrukerprofil(kariIdent)
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe kariIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.AKTIV
                    profil.stillingssoek.shouldHaveSize(1)
                    profil.stillingssoek.firstOrNull() should { søk ->
                        søk.shouldNotBeNull()
                        søk.shouldBeInstanceOf<ApiStedSoek>()
                        assertStedSoek(søk, "Viken", "30", "Drammen", "3005")
                    }

                }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }
            "Når Kari ikke lenger er arbeidssøker endres tjenestestatus til ${ApiTjenesteStatus.INAKTIV}, men søket beholdes" {
                testLogger.info("Starter: ${this.testCase.name.name}")
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
                val response = testClient.getBrukerprofil(kariIdent)
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe kariIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                    profil.stillingssoek.shouldHaveSize(1)
                }
                testLogger.info("Avslutter: ${this.testCase.name.name}")
            }
            } // end with(helper)
        }
    }
})