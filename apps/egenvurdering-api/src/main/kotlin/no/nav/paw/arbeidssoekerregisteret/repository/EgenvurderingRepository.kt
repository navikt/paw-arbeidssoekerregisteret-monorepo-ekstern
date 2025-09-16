package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.time.Instant
import java.util.*

interface EgenvurderingRepository {
    fun lagrePerioderOgProfileringer(records: ConsumerRecords<Long, SpecificRecord>)
    fun finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident: String): NyesteProfilering?
    fun lagreEgenvurdering(egenvurdering: Egenvurdering): InsertStatement<Number>
}

data class NyesteProfilering(
    val id: UUID,
    val profilertTil: String,
    val tidspunkt: Instant,
)