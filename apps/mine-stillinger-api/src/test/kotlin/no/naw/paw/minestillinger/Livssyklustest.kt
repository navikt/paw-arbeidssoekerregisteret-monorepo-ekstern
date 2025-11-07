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
import no.naw.paw.ledigestillinger.model.FinnStillingerByEgenskaperRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerResponse
import no.naw.paw.ledigestillinger.model.FinnStillingerType
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Kommune
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.PagingResponse
import no.naw.paw.minestillinger.api.ApiStedSoek
import no.naw.paw.minestillinger.api.MineStillingerResponse
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
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
import no.naw.paw.minestillinger.route.ledigeStillingerRoute
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
    val ledigeStillingerClient: FinnStillingerClient = mockk()
    coEvery{ ledigeStillingerClient.finnLedigeStillinger(any(), any()) }.returns(
        FinnStillingerResponse(
            stillinger = emptyList(),
            paging = PagingResponse()
        )
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
                ledigeStillingerRoute(
                    ledigeStillingerClient = ledigeStillingerClient,
                    hentBrukerId = { identitetsnummer ->
                        brukerprofilTjeneste.hentLokalBrukerProfilEllerNull(
                            identitetsnummer
                        )?.id
                    },
                    hentLagretSøk = ExposedSøkAdminOps::hentSoek,
                    oppdaterSistKjøt = ExposedSøkAdminOps::settSistKjørt,
                    clock = clock
                )
            }

            with(helper) {
                "Bruker som ikke finnes får 404 ved henting av ledigestillinger" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.mineLedigeStillinger(Identitetsnummer("99999999999"))
                    response.status shouldBe HttpStatusCode.NotFound
                    coVerify(exactly = 0) { pdlClient.harBeskyttetAdresse(any()) }
                    coVerify(exactly = 0) { ledigeStillingerClient.finnLedigeStillinger(any(), any()) }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }
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
                    "Rolf får 404 på mine stillinger siden ingen søk er lagret" {
                        testLogger.info("Starter: ${this.testCase.name.name}")
                        val response = testClient.mineLedigeStillinger(rolfIdent)
                        response.status shouldBe HttpStatusCode.NotFound
                        coVerify(exactly = 0) { ledigeStillingerClient.finnLedigeStillinger(any(), any()) }
                        testLogger.info("Avslutter: ${this.testCase.name.name}")
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
                "Kari får 404 på mine stillinger før søket er lagret" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.mineLedigeStillinger(kariIdent)
                    response.status shouldBe HttpStatusCode.NotFound
                    coVerify(exactly = 0) { ledigeStillingerClient.finnLedigeStillinger(any(), any()) }
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
                val karisLedigeStillingerRequest = FinnStillingerByEgenskaperRequest(
                    type = FinnStillingerType.BY_EGENSKAPER,
                    soekeord = listOf("Hei AS"),
                    kategorier = listOf("1234"),
                    fylker = listOf(
                        Fylke(
                            "30",
                            kommuner = listOf(Kommune("3005"))
                        )
                    ),
                    paging = Paging(1, 10)
                )
                "Kari kan nå hente ut ledige stillinger, men ingen er tilgjengelig" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val response = testClient.mineLedigeStillinger(kariIdent)
                    response.status shouldBe HttpStatusCode.OK
                    response.body<MineStillingerResponse>() should { data ->
                        data.shouldNotBeNull()
                        data.resultat.shouldBeEmpty()
                    }
                    coVerify(exactly = 1) { ledigeStillingerClient.finnLedigeStillinger(any(), any()) }
                    testLogger.info("Avslutter: ${this.testCase.name.name}")
                }

                "Når en stillinger blir tilgjengelig dukker den opp i søket til Kari" {
                    testLogger.info("Starter: ${this.testCase.name.name}")
                    val stilling = lagStilling(karisLedigeStillingerRequest)
                    coEvery { ledigeStillingerClient.finnLedigeStillinger(any(), any()) } returns FinnStillingerResponse(
                        stillinger = listOf(stilling),
                        paging = PagingResponse()
                    )
                    val response = testClient.mineLedigeStillinger(kariIdent)
                    response.status shouldBe HttpStatusCode.OK
                    response.body<MineStillingerResponse>() should { data ->
                        data.shouldNotBeNull()
                        data.resultat.size shouldBe 1
                        data.resultat.first() should { annonse ->
                            annonse.soeknadsfrist.type.name.lowercase() shouldBe stilling.soeknadsfrist.type.name.lowercase()
                            annonse.tittel shouldBe stilling.tittel
                            annonse.arbeidsplassenNoId shouldBe stilling.uuid
                            annonse.selskap shouldBe stilling.arbeidsgivernavn
                            annonse.sektor.name.lowercase() shouldBe stilling.sektor.name.lowercase()
                        }
                    }
                    coVerify(exactly = 2) { ledigeStillingerClient.finnLedigeStillinger(any(), any()) }
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
                            createProfilering(
                                periodeId = olaPeriode.id,
                                profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
                            )
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