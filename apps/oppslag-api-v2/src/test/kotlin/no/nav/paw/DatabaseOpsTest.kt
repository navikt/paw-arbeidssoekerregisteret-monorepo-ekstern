package no.nav.paw

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStart
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStopp
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.config.env.Local
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.consumer.writeBatchToDb
import no.nav.paw.oppslagapi.data.opplysninger_om_arbeidssoeker_v4
import no.nav.paw.oppslagapi.data.pa_vegne_av_start_v1
import no.nav.paw.oppslagapi.data.pa_vegne_av_stopp_v1
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.data.query.ExposedDatabaseQuerySupport
import no.nav.paw.oppslagapi.initDatabase
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

const val database = "paw"
const val username = "paw_user"
const val password = "paw_password"
val topicNames = standardTopicNames(Local)
val serde: Serde<SpecificRecord> = opprettSerde()

class DatabaseOpsTest : FreeSpec({
    "Database Ops" - {
        val dbContainer = PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withDatabaseName(database)
            .withUsername(username)
            .withPassword(password)
            .withExposedPorts(5432)
            .waitingFor(HostPortWaitStrategy())
        dbContainer.start()
        val dbConfig = DatabaseConfig(
            username = username,
            password = password,
            database = database,
            host = dbContainer.host,
            port = dbContainer.getMappedPort(5432)
        )

        initDatabase(topicNames, dbConfig)
        "Vi kan skrive testdata til db" {
            transaction {
                writeBatchToDb(TestData.dataRows.asSequence())
            }
        }

        "Verifiser spørringer på test data" - {
            "Vi kan hente id til periode A via identitetsnummer" {
                val perioder =
                    ExposedDatabaseQuerySupport.hentPerioder(Identitetsnummer(TestData.periode_a_startet.identitetsnummer))
                perioder.size shouldBe 1
                perioder.first() shouldBe TestData.periode_a_startet.id
            }
            "Vi kan hente rader for periode A via periodeId" {
                val rader = ExposedDatabaseQuerySupport.hentRaderForPeriode(TestData.periode_a_startet.id)
                rader.size shouldBe 6
                rader.firstOrNull { it.type == pa_vegne_av_start_v1 } should { paaVegneAvStart ->
                    paaVegneAvStart.shouldNotBeNull()
                    paaVegneAvStart.periodeId shouldBe TestData.periode_a_paa_vegne_av_startet.periodeId
                    paaVegneAvStart.identitetsnummer shouldBe null
                    paaVegneAvStart.data should { data ->
                        data.shouldBeInstanceOf<PaaVegneAvStart>()
                        data.graceMS shouldBe (TestData.periode_a_paa_vegne_av_startet.handling as? Start)?.graceMS
                        data.intervalMS shouldBe (TestData.periode_a_paa_vegne_av_startet.handling as? Start)?.intervalMS
                    }
                }
                rader.firstOrNull { it.type == pa_vegne_av_stopp_v1 } should { paaVegneAvStopp ->
                    paaVegneAvStopp.shouldNotBeNull()
                    paaVegneAvStopp.periodeId shouldBe TestData.periode_a_paa_vegne_av_stoppet.periodeId
                    paaVegneAvStopp.identitetsnummer shouldBe null
                    paaVegneAvStopp.data should { data ->
                        data.shouldBeInstanceOf<PaaVegneAvStopp>()
                        data.periodeId shouldBe TestData.periode_a_paa_vegne_av_stoppet.periodeId
                        data.bekreftelsesloesning.name shouldBe TestData.periode_a_paa_vegne_av_stoppet.bekreftelsesloesning.name
                        data.fristBrutt shouldBe (TestData.periode_a_paa_vegne_av_stoppet.handling as? Stopp)?.fristBrutt
                    }
                }
                rader.firstOrNull { it.type == bekreftelsemelding_v1 } should { bekreftelse ->
                    bekreftelse.shouldNotBeNull()
                    bekreftelse.periodeId shouldBe TestData.periode_a_bekreftelse.periodeId
                    bekreftelse.timestamp shouldBe TestData.periode_a_bekreftelse.svar.sendtInnAv.tidspunkt
                    bekreftelse.identitetsnummer shouldBe null
                    bekreftelse.data should { data ->
                        data.shouldBeInstanceOf<Bekreftelse>()
                        data.periodeId shouldBe TestData.periode_a_bekreftelse.periodeId
                        data.bekreftelsesloesning.name shouldBe TestData.periode_a_bekreftelse.bekreftelsesloesning.name
                        data.svar.gjelderTil shouldBe TestData.periode_a_bekreftelse.svar.gjelderTil
                        data.svar.gjelderFra shouldBe TestData.periode_a_bekreftelse.svar.gjelderFra
                        data.svar.harJobbetIDennePerioden shouldBe TestData.periode_a_bekreftelse.svar.harJobbetIDennePerioden
                        data.svar.vilFortsetteSomArbeidssoeker shouldBe data.svar.vilFortsetteSomArbeidssoeker
                        data.svar.sendtInnAv shouldMatch TestData.periode_a_bekreftelse.svar.sendtInnAv
                    }
                }
                rader.firstOrNull { it.type == periode_startet_v1 } should { periodeStart ->
                    periodeStart.shouldNotBeNull()
                    periodeStart.timestamp shouldBe TestData.periode_a_startet.startet.tidspunkt
                    periodeStart.periodeId shouldBe TestData.periode_a_startet.id
                    periodeStart.identitetsnummer shouldBe TestData.periode_a_startet.identitetsnummer
                    periodeStart.data shouldMatch TestData.periode_a_startet.startet
                }
                rader.firstOrNull { it.type == opplysninger_om_arbeidssoeker_v4 } should { opplysninger ->
                    opplysninger.shouldNotBeNull()
                    opplysninger.timestamp shouldBe TestData.periode_a_opplysninger.sendtInnAv.tidspunkt
                    opplysninger.periodeId shouldBe TestData.periode_a_opplysninger.periodeId
                    opplysninger.timestamp shouldBe TestData.periode_a_opplysninger.sendtInnAv.tidspunkt
                    opplysninger.identitetsnummer shouldBe null
                    opplysninger.data shouldMatch TestData.periode_a_opplysninger
                }
                rader.firstOrNull { it.type == periode_avsluttet_v1 } should { periodeAvsluttet ->
                    periodeAvsluttet.shouldNotBeNull()
                    periodeAvsluttet.timestamp shouldBe TestData.periode_a_avsluttet.avsluttet.tidspunkt
                    periodeAvsluttet.periodeId shouldBe TestData.periode_a_avsluttet.id
                    periodeAvsluttet.identitetsnummer shouldBe TestData.periode_a_avsluttet.identitetsnummer
                    periodeAvsluttet.data shouldMatch TestData.periode_a_avsluttet.avsluttet
                }
            }
            "Vi kan hente id til periode B og C via identitetsnummer" {
                val perioder = ExposedDatabaseQuerySupport.hentPerioder(Identitetsnummer(TestData.periode_b_startet.identitetsnummer))
                perioder.size shouldBe 2
                perioder shouldContain TestData.periode_b_startet.id
                perioder shouldContain TestData.periode_c_startet.id
            }
            "Vi kan hente rader for periode B via periodeId" {
                val rader = ExposedDatabaseQuerySupport.hentRaderForPeriode(TestData.periode_b_startet.id)
                rader.size shouldBe 3
                rader.firstOrNull { it.type == periode_startet_v1 } should { periodeStart ->
                    periodeStart.shouldNotBeNull()
                    periodeStart.timestamp shouldBe TestData.periode_b_startet.startet.tidspunkt
                    periodeStart.periodeId shouldBe TestData.periode_b_startet.id
                    periodeStart.identitetsnummer shouldBe TestData.periode_b_startet.identitetsnummer
                    periodeStart.data shouldMatch TestData.periode_b_startet.startet
                }
                rader.firstOrNull { it.type == opplysninger_om_arbeidssoeker_v4 } should { opplysninger ->
                    opplysninger.shouldNotBeNull()
                    opplysninger.periodeId shouldBe TestData.periode_b_opplysninger.periodeId
                    opplysninger.timestamp shouldBe TestData.periode_b_opplysninger.sendtInnAv.tidspunkt
                    opplysninger.identitetsnummer shouldBe null
                    opplysninger.data shouldMatch TestData.periode_b_opplysninger
                }
                rader.firstOrNull { it.type == periode_avsluttet_v1 } should { periodeAvsluttet ->
                    periodeAvsluttet.shouldNotBeNull()
                    periodeAvsluttet.timestamp shouldBe TestData.periode_b_avsluttet.avsluttet.tidspunkt
                    periodeAvsluttet.periodeId shouldBe TestData.periode_b_avsluttet.id
                    periodeAvsluttet.identitetsnummer shouldBe TestData.periode_b_avsluttet.identitetsnummer
                    periodeAvsluttet.data shouldMatch TestData.periode_b_avsluttet.avsluttet
                }
            }
        }
    }
})

infix fun Any.shouldMatch(opplysninger: OpplysningerOmArbeidssoeker) {
    this.shouldBeInstanceOf<no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.OpplysningerOmArbeidssoeker>()
    id shouldBe opplysninger.id
    periodeId shouldBe opplysninger.periodeId
    annet?.andreForholdHindrerArbeid?.name shouldBe opplysninger.annet?.andreForholdHindrerArbeid?.name
    helse?.helsetilstandHindrerArbeid?.name shouldBe opplysninger.helse?.helsetilstandHindrerArbeid?.name
    utdanning?.bestaatt?.name shouldBe opplysninger.utdanning?.bestaatt?.name
    utdanning?.nus shouldBe opplysninger.utdanning?.nus
    utdanning?.godkjent?.name shouldBe opplysninger.utdanning?.godkjent?.name
    jobbsituasjon?.beskrivelser?.size shouldBe opplysninger.jobbsituasjon?.beskrivelser?.size
}

infix fun Any.shouldMatch(metadata: no.nav.paw.arbeidssokerregisteret.api.v1.Metadata) {
    this.shouldBeInstanceOf<Metadata>()
    tidspunkt shouldBe metadata.tidspunkt
    utfoertAv.id shouldBe metadata.utfoertAv.id
    utfoertAv.type.name shouldBe metadata.utfoertAv.type.name
    kilde shouldBe metadata.kilde
    tidspunktFraKilde?.tidspunkt shouldBe metadata.tidspunktFraKilde?.tidspunkt
    tidspunktFraKilde?.avviksType?.name shouldBe metadata.tidspunktFraKilde?.avviksType?.name
}

infix fun Any.shouldMatch(metadata: no.nav.paw.bekreftelse.melding.v1.vo.Metadata) {
    this.shouldBeInstanceOf<Metadata>()
    tidspunkt shouldBe metadata.tidspunkt
    utfoertAv.id shouldBe metadata.utfoertAv.id
    utfoertAv.type.name shouldBe metadata.utfoertAv.type.name
    kilde shouldBe metadata.kilde
}
