package no.nav.paw.ledigestillinger.model.dao

import no.naw.paw.ledigestillinger.model.StillingStatus
import no.naw.paw.ledigestillinger.model.VisningGrad
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
    val stillingstittel: String?,
    val ansettelsesform: String?,
    val stillingsprosent: String?,
    val stillingsantall: String?,
    val sektor: String?,
    val soeknadsfrist: String?,
    val oppstartsfrist: String?,
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
