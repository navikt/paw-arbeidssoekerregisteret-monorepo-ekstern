package no.nav.arbeidssokerregisteret.arena.adapter.utils

import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v3.Utdanning
import org.apache.avro.specific.SpecificRecord
import java.time.Instant
import java.util.UUID

fun periode(
    identietsnummer: String,
    id: UUID = UUID.randomUUID(),
    startet: Metadata = metadata(Instant.now()),
    avsluttet: Metadata? = null
) = Periode(
    id,
    identietsnummer,
    startet,
    avsluttet
)

fun opplysninger(
    id: UUID = UUID.randomUUID(),
    periode: UUID = UUID.randomUUID(),
    timestamp: Instant = Instant.now(),
    nus: String = "1",
    beskrivelse: List<Beskrivelse> = listOf(Beskrivelse.AKKURAT_FULLFORT_UTDANNING)
) = OpplysningerOmArbeidssoeker(
    id,
    periode,
    metadata(timestamp),
    utdanning(nus),
    Helse(JaNeiVetIkke.NEI),
    Arbeidserfaring(JaNeiVetIkke.NEI),
    jobbSituasjon(beskrivelse),
    Annet(JaNeiVetIkke.NEI)
)

fun profilering(
    id: UUID = UUID.randomUUID(),
    opplysningerId: UUID = UUID.randomUUID(),
    periode: UUID = UUID.randomUUID(),
    timestamp: Instant = Instant.now(),
    profilertTil: ProfilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
) = Profilering(
    id,
    periode,
    opplysningerId,
    metadata(timestamp),
    profilertTil,
    true,
    42
)

fun metadata(timestamp: Instant): Metadata {
    return Metadata(
        timestamp,
        Bruker(
            BrukerType.SYSTEM,
            ""
        ),
        "",
        ""
    )
}

fun jobbSituasjon(beskrivelser: List<Beskrivelse> = listOf(Beskrivelse.AKKURAT_FULLFORT_UTDANNING)) =
    Jobbsituasjon(
        beskrivelser.map { beskrivelse ->
            BeskrivelseMedDetaljer(
                beskrivelse,
                mapOf(
                    Pair("test", "test"),
                    Pair("test2", "test2")
                )
            )

        }
    )

fun utdanning(
    nus: String = "9",
    bestaatt: JaNeiVetIkke = JaNeiVetIkke.JA,
    godkjent: JaNeiVetIkke = JaNeiVetIkke.JA
) = Utdanning(
    nus,
    bestaatt,
    godkjent
)

val Pair<Long, *>.key get() : Long = first
val <A: SpecificRecord> Pair<Long, A>.melding get(): A = second