package no.nav.paw.oppslagapi.mapping.v2

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.oppslagapi.data.consumer.converters.toOpenApi
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.paw.test.data.periode.createAnnet
import no.nav.paw.test.data.periode.createHelse
import no.nav.paw.test.data.periode.createOpplysninger
import no.nav.paw.test.data.periode.createProfilering
import no.nav.paw.test.data.periode.createUtdanning
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as MainAvroMetadata
import no.nav.paw.oppslagapi.model.v2.Metadata as OpenApiMetadata

class MainAvroConvertersTest : FreeSpec({
    "MainAvroConverters" - {
        "Verifiser at Avro Periode konverteres riktig til Open API Periode" - {
            listOf(
                PeriodeFactory.create().build(),
                PeriodeFactory.create().build(avsluttet = MetadataFactory.create().build())
            ).map { it to it.toOpenApi() }
                .forEach { (avroPeriode, openApiPeriode) ->
                    "Open API Periode($openApiPeriode) skal være lik Avro Periode($avroPeriode)" {
                        openApiPeriode.id shouldBe avroPeriode.id
                        openApiPeriode.identitetsnummer shouldBe avroPeriode.identitetsnummer
                        openApiPeriode.startet shouldMatch avroPeriode.startet
                        openApiPeriode.avsluttet shouldMatch avroPeriode.avsluttet
                    }
                }

        }
        "Verifiser av Avro Opplysninger Om Arbeidssoker konverteres riktig til Open API Opplysninger" - {
            listOf(
                createOpplysninger(
                    annet = null,
                    helse = null,
                    utdanning = null
                ),
                createOpplysninger(
                    annet = createAnnet(true),
                    helse = createHelse(false),
                    utdanning = createUtdanning(nus = "0", godkjent = false, bestatt = true)
                ),
                createOpplysninger(
                    annet = createAnnet(false),
                    helse = createHelse(true),
                    utdanning = createUtdanning(nus = "2334", godkjent = true, bestatt = false)
                ),
                createOpplysninger(
                    annet = createAnnet(null),
                    helse = createHelse(true),
                    utdanning = createUtdanning(nus = "2334", godkjent = true, bestatt = null)
                ),
                createOpplysninger(
                    annet = createAnnet(false),
                    helse = createHelse(null),
                    utdanning = createUtdanning(nus = "5432", godkjent = null, bestatt = false)
                )
            ).map { it to it.toOpenApi() }
                .forEach { (avroOpplysninger, openApiOpplysninger) ->
                    "Open API Opplysninger($openApiOpplysninger) skal være lik Avro Opplysninger($avroOpplysninger)" {
                        openApiOpplysninger.id shouldBe avroOpplysninger.id
                        openApiOpplysninger.periodeId shouldBe avroOpplysninger.periodeId
                        openApiOpplysninger.helse?.helsetilstandHindrerArbeid?.name shouldBe avroOpplysninger.helse?.helsetilstandHindrerArbeid?.name
                        openApiOpplysninger.annet?.andreForholdHindrerArbeid?.name shouldBe avroOpplysninger.annet?.andreForholdHindrerArbeid?.name
                        openApiOpplysninger.sendtInnAv shouldMatch avroOpplysninger.sendtInnAv
                        openApiOpplysninger.jobbsituasjon should { jobbsituasjon ->
                            jobbsituasjon?.beskrivelser?.size shouldBe avroOpplysninger.jobbsituasjon?.beskrivelser?.size
                            jobbsituasjon?.beskrivelser?.forEach { beskrivelse ->
                                val avroBeskrivelse = avroOpplysninger.jobbsituasjon?.beskrivelser
                                    ?.firstOrNull { it.beskrivelse.name == beskrivelse.beskrivelse.name }
                                    .shouldNotBeNull()
                                beskrivelse.beskrivelse.name shouldBe avroBeskrivelse.beskrivelse.name
                                beskrivelse.detaljer should { beskrivelse ->
                                    beskrivelse.size shouldBe avroBeskrivelse.detaljer.size
                                    beskrivelse.forEach { (key, value) ->
                                        avroBeskrivelse.detaljer[key] shouldBe value
                                    }
                                }
                            }
                        }
                        openApiOpplysninger.utdanning?.nus shouldBe avroOpplysninger.utdanning?.nus
                        openApiOpplysninger.utdanning?.bestaatt?.name shouldBe avroOpplysninger.utdanning?.bestaatt?.name
                        openApiOpplysninger.utdanning?.godkjent?.name shouldBe avroOpplysninger.utdanning?.godkjent?.name
                    }
                }
        }
        "Verifiser av Avro Profilering konverteres riktig til Open API Profilering" - {
            listOf(
                createProfilering(
                    profilertTil = ProfilertTil.OPPGITT_HINDRINGER,
                    alder = 64,
                    jobbetSammenhengende = false
                ),
                createProfilering(
                    profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
                    alder = 33,
                    jobbetSammenhengende = true
                ),
                createProfilering()
            ).map { it to it.toOpenApi() }
                .forEach { (avroProfilering, openApiProfilering) ->
                    "Open API Profilering($openApiProfilering) skal være lik Avro Profilering($avroProfilering)" {
                        openApiProfilering.id shouldBe avroProfilering.id
                        openApiProfilering.periodeId shouldBe avroProfilering.periodeId
                        openApiProfilering.opplysningerOmArbeidssokerId shouldBe avroProfilering.opplysningerOmArbeidssokerId
                        openApiProfilering.jobbetSammenhengendeSeksAvTolvSisteMnd shouldBe avroProfilering.jobbetSammenhengendeSeksAvTolvSisteMnd
                        openApiProfilering.sendtInnAv shouldMatch avroProfilering.sendtInnAv
                        openApiProfilering.alder shouldBe avroProfilering.alder
                        openApiProfilering.profilertTil.name shouldBe avroProfilering.profilertTil.name
                    }
                }
        }
    }
})

infix fun OpenApiMetadata?.shouldMatch(metadata: MainAvroMetadata?) {
    this?.tidspunkt shouldBe metadata?.tidspunkt
    this?.kilde shouldBe metadata?.kilde
    this?.aarsak shouldBe metadata?.aarsak
    this?.tidspunktFraKilde should {
        it?.tidspunkt shouldBe metadata?.tidspunktFraKilde?.tidspunkt
        it?.avviksType?.name shouldBe metadata?.tidspunktFraKilde?.avviksType?.name
    }
    this?.utfoertAv?.id shouldBe metadata?.utfoertAv?.id
    this?.utfoertAv?.type?.name shouldBe metadata?.utfoertAv?.type?.name
    this?.utfoertAv?.sikkerhetsnivaa shouldBe metadata?.utfoertAv?.sikkerhetsnivaa
}
