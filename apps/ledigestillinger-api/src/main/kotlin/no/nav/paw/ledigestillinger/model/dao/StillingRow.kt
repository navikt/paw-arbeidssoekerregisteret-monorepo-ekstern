package no.nav.paw.ledigestillinger.model.dao

import no.nav.paw.ledigestillinger.model.StillingStatus
import no.nav.paw.ledigestillinger.model.VisningGrad
import org.jetbrains.exposed.v1.core.ResultRow
import java.time.Instant
import java.util.*

data class StillingRow(
    val id: Long,
    val uuid: UUID,
    val adnr: String?,
    val tittel: String,
    val status: StillingStatus,
    val visning: VisningGrad,
    val kilde: String,
    val medium: String,
    val referanse: String,
    val arbeidsgivernavn: String?,
    val opprettetTimestamp: Instant,
    val endretTimestamp: Instant,
    val publisertTimestamp: Instant,
    val utloeperTimestamp: Instant?,
    val messageTimestamp: Instant,
    val insertTimestamp: Instant,
    val updatedTimestamp: Instant?,
    val arbeidsgiver: ArbeidsgiverRow?,
    val kategorier: Iterable<KategoriRow>,
    val klassifiseringer: Iterable<KlassifiseringRow>,
    val lokasjoner: Iterable<LokasjonRow>,
    val egenskaper: Iterable<EgenskapRow>
)

fun ResultRow.asStillingRow(
    arbeidsgiver: (Long) -> ArbeidsgiverRow?,
    kategorier: (Long) -> Iterable<KategoriRow>,
    klassifiseringer: (Long) -> Iterable<KlassifiseringRow>,
    lokasjoner: (Long) -> Iterable<LokasjonRow>,
    egenskaper: (Long) -> Iterable<EgenskapRow>
): StillingRow {
    val id = this[StillingerTable.id].value
    return StillingRow(
        id = id,
        uuid = this[StillingerTable.uuid],
        adnr = this[StillingerTable.adnr],
        tittel = this[StillingerTable.tittel],
        status = this[StillingerTable.status],
        visning = this[StillingerTable.visning],
        kilde = this[StillingerTable.kilde],
        medium = this[StillingerTable.medium],
        referanse = this[StillingerTable.referanse],
        arbeidsgivernavn = this[StillingerTable.arbeidsgivernavn],
        opprettetTimestamp = this[StillingerTable.opprettetTimestamp],
        endretTimestamp = this[StillingerTable.endretTimestamp],
        publisertTimestamp = this[StillingerTable.publisertTimestamp],
        utloeperTimestamp = this[StillingerTable.utloeperTimestamp],
        messageTimestamp = this[StillingerTable.messageTimestamp],
        insertTimestamp = this[StillingerTable.insertTimestamp],
        updatedTimestamp = this[StillingerTable.updatedTimestamp],
        arbeidsgiver = arbeidsgiver(id),
        kategorier = kategorier(id),
        klassifiseringer = klassifiseringer(id),
        lokasjoner = lokasjoner(id),
        egenskaper = egenskaper(id)
    )
}