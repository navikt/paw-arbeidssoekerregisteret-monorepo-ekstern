package no.nav.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.arena.v1.Annet
import no.nav.paw.arbeidssokerregisteret.arena.v1.Arbeidserfaring
import no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.arena.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.arena.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.arena.v1.Helse
import no.nav.paw.arbeidssokerregisteret.arena.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.arena.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.arena.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.arena.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.v1.Periode
import no.nav.paw.arbeidssokerregisteret.arena.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.arena.v3.Utdanning
import java.time.Instant
import java.util.*

object TestData {
    private val testPeriodeId1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val testPeriodeId2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val testOpplysningerId1 = UUID.fromString("00000000-0000-0000-0000-000000000003")

    val perioder = listOf(
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
                "test"
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
                "test"
            ),
            Metadata(
                Instant.now().plusSeconds(100),
                Bruker(
                    BrukerType.UKJENT_VERDI,
                    "12345678911"
                ),
                "test",
                "test"
            )
        )
    )

    val opplysningerOmArbeidssoeker = listOf(
        OpplysningerOmArbeidssoeker(
            testOpplysningerId1,
            testPeriodeId1,
            Metadata(
                Instant.now(),
                Bruker(
                    BrukerType.UKJENT_VERDI,
                    "12345678911"
                ),
                "test",
                "test"
            ),
            Utdanning(
                "1",
                JaNeiVetIkke.JA,
                JaNeiVetIkke.JA
            ),
            Helse(
                JaNeiVetIkke.JA
            ),
            Arbeidserfaring(
                JaNeiVetIkke.JA
            ),
            Jobbsituasjon(
                listOf(
                    BeskrivelseMedDetaljer(
                        Beskrivelse.AKKURAT_FULLFORT_UTDANNING,
                        mapOf(
                            Pair("test", "test"),
                            Pair("test2", "test2")
                        )
                    ),
                    BeskrivelseMedDetaljer(
                        Beskrivelse.DELTIDSJOBB_VIL_MER,
                        mapOf(
                            Pair("test3", "test3"),
                            Pair("test4", "test4")
                        )
                    )
                )
            ),
            Annet(
                JaNeiVetIkke.JA
            )
        )
    )

    val profilering = listOf(
        Profilering(
            UUID.randomUUID(),
            testPeriodeId1,
            testOpplysningerId1,
            Metadata(
                Instant.now(),
                Bruker(
                    BrukerType.UKJENT_VERDI,
                    "12345678911"
                ),
                "test",
                "test"
            ),
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
            true,
            30
        )
    )
}