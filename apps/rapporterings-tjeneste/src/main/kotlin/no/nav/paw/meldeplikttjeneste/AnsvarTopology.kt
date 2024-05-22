package no.nav.paw.meldeplikttjeneste

import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.meldeplikttjeneste.tilstand.Ansvarlig
import no.nav.paw.meldeplikttjeneste.tilstand.InternTilstand
import no.nav.paw.meldeplikttjeneste.tilstand.Regler
import no.nav.paw.rapportering.ansvar.v1.AnsvarEndret
import no.nav.paw.rapportering.ansvar.v1.AvslutterAnsvar
import no.nav.paw.rapportering.ansvar.v1.TarAnsvar
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelse
import no.nav.paw.rapportering.internehendelser.RapporteringsMeldingMottatt
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Duration.ofMillis
import java.util.UUID

context(ApplicationConfiguration, ApplicationContext)
fun StreamsBuilder.processAnsvarTopic() {
    stream<Long, AnsvarEndret>(ansvarsTopic)
        .genericProcess<Long, AnsvarEndret, Long, RapporteringsHendelse>("ansvarEndret", statStoreName) { record ->
            val keyValueStore: KeyValueStore<UUID, InternTilstand> = getStateStore(statStoreName)
            val ansvarEndret = record.value()
            val internTilstand = keyValueStore[ansvarEndret.periodeId] ?: return@genericProcess
            val (nyTilstand, hendelser) = when (val handling = ansvarEndret.handling) {
                is TarAnsvar -> {
                    val ansvarlig = ansvarEndret.ansvarlig(handling)
                    val harAnsvarIdag = internTilstand.harAnsvar(ansvarlig)
                    val nyTilstand = internTilstand.addOrUpdate(ansvarlig)
                    if (harAnsvarIdag) {
                        nyTilstand to emptyList<RapporteringsHendelse>()
                    }
                    else {
                        val utgaaendeMeldinger = nyTilstand.utestaaende.map { utestaaende ->
                            RapporteringsMeldingMottatt(
                                hendelseId = UUID.randomUUID(),
                                periodeId = ansvarEndret.periodeId,
                                identitetsnummer = internTilstand.periode.identitetsnummer,
                                arbeidssoekerId = internTilstand.periode.kafkaKeysId,
                                rapporteringsId = utestaaende.rapporteringsId,
                                fortsetterSomArbeidssoker = true
                            )
                        }
                        val nyTilstandUtenUtestaaende = nyTilstand.copy(utestaaende = emptyList())
                        nyTilstandUtenUtestaaende to utgaaendeMeldinger
                    }
                }
                is AvslutterAnsvar -> internTilstand.remove(ansvarEndret.namespace, ansvarEndret.id) to emptyList()
                else -> throw IllegalArgumentException("Ukjent handling: $handling")
            }
            keyValueStore.put(ansvarEndret.periodeId, nyTilstand)
            hendelser.map { hendelse ->
                record.withValue(hendelse)
            }.forEach(::forward)
        }
}

fun AnsvarEndret.ansvarlig(tarAnsvar: TarAnsvar) = Ansvarlig(namespace, id, Regler(
    interval = ofMillis(tarAnsvar.intervalMS),
    gracePeriode = ofMillis(tarAnsvar.graceMS)
))

fun InternTilstand.harAnsvar(ansvarlig: Ansvarlig) =
    ansvarlige.any { it.namespace == ansvarlig.namespace && it.id == ansvarlig.id }

fun InternTilstand.addOrUpdate(ansvarlig: Ansvarlig): InternTilstand =
    copy(ansvarlige = ansvarlige.filterNot { it.namespace == ansvarlig.namespace && it.id == ansvarlig.id } + ansvarlig)

fun InternTilstand.remove(namespace: String, id: String): InternTilstand =
    copy(ansvarlige = ansvarlige.filterNot { it.namespace == namespace && it.id == id })