package no.naw.paw.ledigestillinger.model.dao

import java.time.Instant
import java.time.LocalDate
import java.util.*

data class StillingRow(
    val id: Long,
    val uuid: UUID,
    val tittel: String,
    val beskrivelse: String,
    val status: String, // TODO: Enum?
    val kilde: String,
    val startDate: LocalDate,
    val annonseUrl: String,
    val publisertTimestamp: Instant,
    val utloeperTimestamp: Instant?,
    val endretTimestamp: Instant,
    val metadata: MetadataRow,
    val klassifiseringer: Iterable<KlassifiseringRow>,
    val arbeidsgivere: Iterable<ArbeidsgiverRow>,
    val beliggenheter: Iterable<BeliggenhetRow>
)
