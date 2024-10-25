package no.nav.paw.arbeidssoekerregisteret.eksternt.api.kafka

import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.time.Instant
import java.util.*

class PeriodeProducerUtils {
    val testPeriodeId1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val testPeriodeId2 = UUID.fromString("00000000-0000-0000-0000-000000000002")

    fun lagTestPerioder(): List<Periode> {
        return listOf(
            Periode(
                testPeriodeId1,
                "12345678911",
                Metadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678911"
                    ),
                    "test",
                    "test",
                    null
                ),
                null
            ),
            Periode(
                testPeriodeId2,
                "12345678911",
                Metadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678911"
                    ),
                    "test",
                    "test",
                    null
                ),
                Metadata(
                    Instant.now().plusSeconds(100),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678911"
                    ),
                    "test",
                    "test",
                    null
                )
            )
        )
    }
}
