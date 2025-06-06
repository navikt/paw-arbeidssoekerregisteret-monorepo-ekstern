package no.nav.paw.oppslagsapi

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.oppslagapi.toOpenApi
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import no.nav.paw.test.data.bekreftelse.startPaaVegneAv
import no.nav.paw.test.data.bekreftelse.stoppPaaVegneAv
import java.time.Duration

class BekreftelseConvertersTest : FreeSpec({
    "BekreftelseConverters" - {
        "Bekreftelse konverteres riktig til Open API Bekreftelse" - {
            listOf(
                bekreftelseMelding(),
                bekreftelseMelding(
                    bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER,
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = false
                ),
                bekreftelseMelding(
                    bekreftelsesloesning = Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING,
                    harJobbetIDennePerioden = false,
                    vilFortsetteSomArbeidssoeker = true
                ),
                bekreftelseMelding(
                    bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = true
                )
            ).map { it to it.toOpenApi() }
                .forEach { (avroBekreftelse, openApiBekreftelse) ->
                    "Open API Bekreftelse($openApiBekreftelse) skal være lik Avro Bekreftelse($avroBekreftelse)" {
                        openApiBekreftelse.id shouldBe avroBekreftelse.id
                        openApiBekreftelse.periodeId shouldBe avroBekreftelse.periodeId
                        openApiBekreftelse.bekreftelsesloesning.name shouldBe avroBekreftelse.bekreftelsesloesning.name
                        openApiBekreftelse.svar.harJobbetIDennePerioden shouldBe avroBekreftelse.svar.harJobbetIDennePerioden
                        openApiBekreftelse.svar.vilFortsetteSomArbeidssoeker shouldBe avroBekreftelse.svar.vilFortsetteSomArbeidssoeker
                        openApiBekreftelse.svar.gjelderTil shouldBe avroBekreftelse.svar.gjelderTil
                        openApiBekreftelse.svar.gjelderFra shouldBe avroBekreftelse.svar.gjelderFra
                    }
                }
        }
        "Avro PaaVegneAv start melding konverteres riktig til Open API PaaVegneAvStart" - {
            listOf(
                startPaaVegneAv(),
                startPaaVegneAv(
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
                    interval = Duration.ofMinutes(1),
                    grace = Duration.ofHours(2)
                ),
                startPaaVegneAv(
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING,
                    interval = Duration.ofDays(11),
                    grace = Duration.ofHours(145)
                ),
                startPaaVegneAv(
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                    interval = Duration.ofMinutes(24),
                    grace = Duration.ofHours(9)
                )
            ).forEach { avroPaaVegneAv ->
                val start = (avroPaaVegneAv.handling as Start)
                val openApiPaaVegneAvStart = start.toOpenApi(avroPaaVegneAv)
                "Open API PaaVegneAvStart($openApiPaaVegneAvStart) skal være lik Avro PaaVegneAvStart($avroPaaVegneAv)" {
                    openApiPaaVegneAvStart.periodeId shouldBe avroPaaVegneAv.periodeId
                    openApiPaaVegneAvStart.intervalMS shouldBe start.intervalMS
                    openApiPaaVegneAvStart.graceMS shouldBe start.graceMS
                    openApiPaaVegneAvStart.bekreftelsesloesning.name shouldBe avroPaaVegneAv.bekreftelsesloesning.name
                }

            }
        }
        "Avro PaaVegneAv start melding konverteres riktig til Open API PaaVegneAvStart" - {
            listOf(
                stoppPaaVegneAv(),
                stoppPaaVegneAv(
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
                    fristBrutt = true
                ),
                stoppPaaVegneAv(
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING,
                    fristBrutt = false
                ),
                stoppPaaVegneAv(
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                    fristBrutt = true
                )

            ).forEach { avroPaaVegneAv ->
                val stopp = (avroPaaVegneAv.handling as Stopp)
                val openApiPaaVegneAvStopp = stopp.toOpenApi(avroPaaVegneAv)
                "Open API PaaVegneAvStart($openApiPaaVegneAvStopp) skal være lik Avro PaaVegneAvStart($avroPaaVegneAv)" {
                    openApiPaaVegneAvStopp.periodeId shouldBe avroPaaVegneAv.periodeId
                    openApiPaaVegneAvStopp.bekreftelsesloesning.name shouldBe avroPaaVegneAv.bekreftelsesloesning.name
                    openApiPaaVegneAvStopp.fristBrutt shouldBe stopp.fristBrutt
                }
            }
        }
    }
})