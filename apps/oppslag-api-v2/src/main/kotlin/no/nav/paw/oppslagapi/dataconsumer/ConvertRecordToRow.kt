package no.nav.paw.oppslagapi.dataconsumer

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.oppslagapi.Row
import no.nav.paw.oppslagapi.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.dataconsumer.converters.toOpenApi
import no.nav.paw.oppslagapi.objectMapper
import no.nav.paw.oppslagapi.opplysninger_om_arbeidssoeker_v4
import no.nav.paw.oppslagapi.pa_vegne_av_start_v1
import no.nav.paw.oppslagapi.pa_vegne_av_stopp_v1
import no.nav.paw.oppslagapi.periode_avsluttet_v1
import no.nav.paw.oppslagapi.periode_startet_v1
import no.nav.paw.oppslagapi.profilering_v1
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Deserializer
import java.time.Instant

fun ConsumerRecord<Long, ByteArray>.toRow(deserializer: Deserializer<SpecificRecord>): Row<String> {
    when (val melding = deserializer.deserialize(topic(), this.value())) {
        is Periode -> {
            val (type, metadata) = if (melding.avsluttet == null) {
                periode_startet_v1 to melding.startet.toOpenApi()
            } else {
                periode_avsluttet_v1 to melding.avsluttet.toOpenApi()
            }
            return Row(
                identitetsnummer = melding.identitetsnummer,
                periodeId = melding.id,
                timestamp = metadata.tidspunkt,
                data = objectMapper.writeValueAsString(metadata),
                type = type
            )
        }

        is OpplysningerOmArbeidssoeker -> {
            return Row(
                identitetsnummer = null,
                periodeId = melding.periodeId,
                timestamp = melding.sendtInnAv.tidspunkt,
                data = objectMapper.writeValueAsString(melding.toOpenApi()),
                type = opplysninger_om_arbeidssoeker_v4
            )
        }

        is Profilering -> {
            return Row(
                identitetsnummer = null,
                periodeId = melding.periodeId,
                timestamp = melding.sendtInnAv.tidspunkt,
                data = objectMapper.writeValueAsString(melding.toOpenApi()),
                type = profilering_v1
            )
        }

        is Bekreftelse -> {
            return Row(
                identitetsnummer = null,
                periodeId = melding.periodeId,
                timestamp = melding.svar.sendtInnAv.tidspunkt,
                data = objectMapper.writeValueAsString(melding.toOpenApi()),
                type = bekreftelsemelding_v1
            )
        }

        is PaaVegneAv -> {
            val (type, openApiObject) = when (val handling = melding.handling) {
                is Start -> pa_vegne_av_start_v1 to handling.toOpenApi(melding)
                is Stopp -> pa_vegne_av_stopp_v1 to handling.toOpenApi(melding)
                else -> throw IllegalArgumentException("PaaVegnaAv handling: ${handling.javaClass}")
            }
            return Row(
                identitetsnummer = null,
                periodeId = melding.periodeId,
                timestamp = Instant.ofEpochMilli(timestamp()),
                data = objectMapper.writeValueAsString(openApiObject),
                type = type
            )
        }
        else -> throw IllegalArgumentException("Unsupported SpecificRecord type: ${this.javaClass}")
    }
}