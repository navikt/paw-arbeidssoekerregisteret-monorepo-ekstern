package no.nav.paw.meldeplikttjeneste.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelse
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass

@JvmRecord
data class InternTilstand(
    val periode: PeriodeInfo,
    val ansvarlige: List<Ansvarlig>,
    val gjedlerTilForSisteInnsendig: Instant?,
    val utestaaende: List<Rapportering>
)

@JvmRecord
data class Rapportering(
    val sisteHandling: KClass<RapporteringsHendelse>,
    val rapporteringsId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant
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
    id: Long,
    key: Long,
    periode: Periode
): InternTilstand =
    InternTilstand(
        gjedlerTilForSisteInnsendig = null,
        periode = PeriodeInfo(
            periodeId = periode.id,
            identitetsnummer = periode.identitetsnummer,
            kafkaKeysId = id,
            recordKey = key,
            avsluttet = periode.avsluttet != null
        ),
        ansvarlige = emptyList(),
        utestaaende = emptyList()
    )

fun List<Ansvarlig>.remove(namespace: String, id: String): List<Ansvarlig> =
    filterNot { it.namespace == namespace && it.id == id }

fun List<Ansvarlig>.addOrUpdate(ansvarlig: Ansvarlig): List<Ansvarlig> =
    remove(ansvarlig.namespace, ansvarlig.id)
        .plus(ansvarlig)