package no.nav.paw.meldeplikttjeneste.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID

@JvmRecord
data class InternTilstand(
    val periode: PeriodeInfo,
    val ansvarlig: List<Ansvarlig> = emptyList(),
    val sisteInnsending: Instant? = null
)

@JvmRecord
data class PeriodeInfo(
    val periodeId: UUID,
    val identitetsnummer: String,
    val kafkaKeysId: Long,
    val recordKey: Long,
    val avsluttet: Boolean
)

@JvmRecord
data class Ansvarlig(
    val namespace: String,
    val id: String,
    val regler: Regler
)

@JvmRecord
data class Regler(
    val interval: Duration,
    val gracePeriode: Duration
)

fun initTilstand(
    kafkaKeysResponse: KafkaKeysResponse,
    periode: Periode
): InternTilstand =
    InternTilstand(
        sisteInnsending = periode.startet.tidspunkt,
        periode = PeriodeInfo(
            periodeId = periode.id,
            identitetsnummer = periode.identitetsnummer,
            kafkaKeysId = kafkaKeysResponse.id,
            recordKey = kafkaKeysResponse.key,
            avsluttet = periode.avsluttet != null
        )
    )

fun initTilstand(
    periodeId: UUID,
    identitetsnummer: String,
    kafkaKeysId: Long,
    recordKey: Long,
    avsluttet: Boolean,
    periodeStartet: Instant
) = InternTilstand(
    sisteInnsending = periodeStartet,
    periode = PeriodeInfo(
        periodeId = periodeId,
        identitetsnummer = identitetsnummer,
        kafkaKeysId = kafkaKeysId,
        recordKey = recordKey,
        avsluttet = avsluttet
    )
)
