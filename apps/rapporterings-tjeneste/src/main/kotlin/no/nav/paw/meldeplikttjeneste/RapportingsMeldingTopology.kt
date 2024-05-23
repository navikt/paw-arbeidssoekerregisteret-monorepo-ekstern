package no.nav.paw.meldeplikttjeneste

import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.meldeplikttjeneste.tilstand.InternTilstand
import no.nav.paw.rapportering.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelse
import no.nav.paw.rapportering.internehendelser.RapporteringsMeldingMottatt
import no.nav.paw.rapportering.melding.v1.Melding
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

context(ApplicationConfiguration, ApplicationContext)
fun StreamsBuilder.processRapporteringsMeldingTopic() {
    stream<Long, Melding>(rapporteringsTopic)
        .genericProcess<Long, Melding, Long, RapporteringsHendelse>(
            name = "meldingMottatt",
            statStoreName
        ) { record ->
            val keyValueStore: KeyValueStore<UUID, InternTilstand> = getStateStore(statStoreName)
            val melding = record.value()
            val tilstand = keyValueStore[record.value().periodeId] ?: return@genericProcess
            val avsenderHarAnsvar = tilstand.harAnsvar(namespace = melding.namespace, id = melding.id)
            val registeretHarAnsvar = tilstand.ansvarlige.isEmpty()
            val utestaaendeMelding = tilstand.finnUtestaaendeMelding(melding)
            when {
                registeretHarAnsvar && utestaaendeMelding != null -> {
                    val nyTilstand = tilstand.copy(
                        utestaaende = tilstand.utestaaende - utestaaendeMelding,
                        gjedlerTilForSisteInnsendig = listOfNotNull(
                            tilstand.gjedlerTilForSisteInnsendig,
                            melding.gjelderTil
                        ).maxOrNull()
                    )
                    nyTilstand to listOfNotNull(
                        RapporteringsMeldingMottatt(
                            hendelseId = UUID.randomUUID(),
                            periodeId = melding.periodeId,
                            identitetsnummer = nyTilstand.periode.identitetsnummer,
                            arbeidssoekerId = nyTilstand.periode.kafkaKeysId,
                            rapporteringsId = utestaaendeMelding.rapporteringsId
                        ),
                        melding
                            .takeUnless { it.vilFortsetteSomArbeidssoeker }
                            ?.let {
                                BaOmAaAvsluttePeriode(
                                    hendelseId = UUID.randomUUID(),
                                    periodeId = melding.periodeId,
                                    identitetsnummer = nyTilstand.periode.identitetsnummer,
                                    arbeidssoekerId = nyTilstand.periode.kafkaKeysId
                                )
                            }
                    )
                }

                !registeretHarAnsvar && utestaaendeMelding == null && avsenderHarAnsvar -> {
                    val nyTilstand = tilstand.copy(
                        gjedlerTilForSisteInnsendig = listOfNotNull(
                            tilstand.gjedlerTilForSisteInnsendig,
                            melding.gjelderTil
                        ).maxOrNull()
                    )
                    nyTilstand to listOfNotNull(melding
                        .takeUnless { it.vilFortsetteSomArbeidssoeker }
                        ?.let {
                            BaOmAaAvsluttePeriode(
                                hendelseId = UUID.randomUUID(),
                                periodeId = melding.periodeId,
                                identitetsnummer = nyTilstand.periode.identitetsnummer,
                                arbeidssoekerId = nyTilstand.periode.kafkaKeysId
                            )
                        }
                    )
                }

                else -> null to emptyList() //TODO: finne ut hvordan vi skal håndtere dette, vi skal egentlig ikke komme hit,

            }
        }
}

fun InternTilstand.harAnsvar(namespace: String, id: String) =
    ansvarlige.any { it.namespace == namespace && it.id == id }

fun InternTilstand.finnUtestaaendeMelding(melding: Melding) =
    utestaaende.firstOrNull { it.gjelderTil == melding.gjelderTil && it.gjelderFra == melding.gjelderFra }
