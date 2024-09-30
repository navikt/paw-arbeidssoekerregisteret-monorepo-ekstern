package no.nav.paw.arbeidssoekerregisteret

import no.nav.common.types.identer.AktorId
import no.nav.paw.arbeidssoekerregisteret.model.Beriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.model.Siste14aVedtak
import no.nav.paw.test.data.periode.BrukerFactory
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import java.time.Instant
import java.util.*

val periodeFactory = PeriodeFactory.create()
val metadataFactory = MetadataFactory.create()
val brukerFactory = BrukerFactory.create()

fun buildPeriode(
    id: UUID = UUID.randomUUID(),
    identitetsnummer: String,
    startetTidspunkt: Instant = Instant.now(),
    avsluttetTidspunkt: Instant? = null
) = periodeFactory.build(
    id = id,
    identitetsnummer = identitetsnummer,
    startet = metadataFactory.build(
        tidspunkt = startetTidspunkt,
        utfortAv = brukerFactory.build(id = identitetsnummer)
    ),
    avsluttetTidspunkt?.let {
        metadataFactory.build(
            tidspunkt = it,
            utfortAv = brukerFactory.build(id = identitetsnummer)
        )
    }
)

fun buildSiste14aVedtak(
    aktorId: String,
    fattetDato: Instant
) = Siste14aVedtak(
    AktorId(aktorId),
    "STANDARD_INNSATS",
    "SKAFFE_ARBEID",
    fattetDato,
    false
)

fun buildBeriket14aVedtak(
    aktorId: String,
    arbeidsoekerId: Long,
    fattetDato: Instant
) = Beriket14aVedtak(
    aktorId,
    arbeidsoekerId,
    "STANDARD_INNSATS",
    "SKAFFE_ARBEID",
    fattetDato,
    false
)
