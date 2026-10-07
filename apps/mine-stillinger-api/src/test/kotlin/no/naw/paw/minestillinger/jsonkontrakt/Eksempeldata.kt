package no.naw.paw.minestillinger.jsonkontrakt

import no.naw.paw.minestillinger.api.ApiJobbAnnonse
import no.naw.paw.minestillinger.api.MineStillingerResponse
import no.naw.paw.minestillinger.api.Sektor
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiFlagg
import no.naw.paw.minestillinger.api.vo.ApiFlaggNavn
import no.naw.paw.minestillinger.api.vo.ApiTag
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import no.naw.paw.minestillinger.domain.Fylke
import no.naw.paw.minestillinger.domain.Kommune
import no.naw.paw.minestillinger.domain.ReiseveiSoek
import no.naw.paw.minestillinger.domain.SoekeTag
import no.naw.paw.minestillinger.domain.StedSoek
import no.naw.paw.minestillinger.domain.api
import no.naw.paw.minestillinger.domain.reiseveiSoek
import no.naw.paw.minestillinger.domain.stedSoek
import no.naw.paw.minestillinger.route.soeknadsfrist
import no.naw.paw.ledigestillinger.model.Frist
import no.naw.paw.ledigestillinger.model.FristType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Faste, deterministiske eksempeldata for JSON-kontrakttestene.
 * Verdiene inngår i fasitfilene og må ikke endres.
 */
object Eksempeldata {
    const val IDENT = "12345678901"
    val TIDSPUNKT_1: Instant = Instant.parse("2026-01-15T10:15:30.123456Z")
    val TIDSPUNKT_2: Instant = Instant.parse("2026-02-01T08:00:00.987654321Z")
    val DATO: LocalDate = LocalDate.parse("2026-03-31")
    val UUID_1: UUID = UUID.fromString("11111111-2222-3333-4444-555555555555")
    val UUID_2: UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
    val UUID_3: UUID = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")
    val UUID_4: UUID = UUID.fromString("fedcba98-7654-3210-fedc-ba9876543210")

    val stedSoekFull: StedSoek = stedSoek(
        fylker = listOf(
            Fylke(
                navn = "Vestland",
                fylkesnummer = "46",
                kommuner = listOf(
                    Kommune(kommunenummer = "4601", navn = "Bergen"),
                    Kommune(kommunenummer = "4627", navn = "Askøy")
                )
            ),
            Fylke(navn = "Oslo", fylkesnummer = "03", kommuner = emptyList())
        ),
        soekeord = listOf("Utvikler", "Kotlin"),
        styrk08 = listOf("2512", "2513"),
        soekeTags = listOf(
            SoekeTag.INGEN_KRAV_TIL_ARBEIDSERFARING_V1,
            SoekeTag.INGEN_KRAV_TIL_UTDANNING_V1,
            SoekeTag.INGEN_KRAV_TIL_FOERERKORT_V1
        )
    )

    val stedSoekTomt: StedSoek = stedSoek(
        fylker = emptyList(),
        soekeord = emptyList(),
        styrk08 = emptyList(),
        soekeTags = emptyList()
    )

    val reiseveiSoekFull: ReiseveiSoek = reiseveiSoek(
        maksAvstandKm = 25,
        postnummer = "0150",
        soekeord = listOf("Sykepleier", "Natt")
    )

    val reiseveiSoekTomt: ReiseveiSoek = reiseveiSoek(
        maksAvstandKm = 0,
        postnummer = "9990",
        soekeord = emptyList()
    )

    val brukerprofilAktiv = ApiBrukerprofil(
        identitetsnummer = IDENT,
        tjenestestatus = ApiTjenesteStatus.AKTIV,
        stillingssoek = listOf(stedSoekFull.api(), reiseveiSoekFull.api()),
        flagg = listOf(
            ApiFlagg(navn = ApiFlaggNavn.TJENESTEN_AKTIVERT, tidspunkt = TIDSPUNKT_1),
            ApiFlagg(navn = ApiFlaggNavn.DIREKTEMELDTE_STILLINGER, tidspunkt = TIDSPUNKT_2),
            ApiFlagg(navn = ApiFlaggNavn.OPT_OUT, tidspunkt = null)
        )
    )

    val brukerprofilInaktivTom = ApiBrukerprofil(
        identitetsnummer = IDENT,
        tjenestestatus = ApiTjenesteStatus.INAKTIV,
        stillingssoek = emptyList(),
        flagg = emptyList()
    )

    val brukerprofilOptOut = ApiBrukerprofil(
        identitetsnummer = IDENT,
        tjenestestatus = ApiTjenesteStatus.OPT_OUT,
        stillingssoek = listOf(stedSoekTomt.api(), reiseveiSoekTomt.api()),
        flagg = listOf(ApiFlagg(navn = ApiFlaggNavn.OPT_OUT, tidspunkt = TIDSPUNKT_1))
    )

    val brukerprofilKanIkkeLeveres = ApiBrukerprofil(
        identitetsnummer = IDENT,
        tjenestestatus = ApiTjenesteStatus.KAN_IKKE_LEVERES,
        stillingssoek = emptyList(),
        flagg = emptyList()
    )

    val jobbAnnonseFull = ApiJobbAnnonse(
        arbeidsplassenNoId = UUID_1,
        tittel = "Kotlin-utvikler",
        stillingbeskrivelse = "Utvikler",
        publisert = TIDSPUNKT_1,
        soeknadsfrist = soeknadsfrist(Frist(type = FristType.DATO, verdi = "31.03.2026", dato = DATO)),
        land = "NORGE",
        kommune = "BERGEN, ASKØY",
        sektor = Sektor.Offentlig,
        selskap = "Eksempel AS",
        tags = ApiTag.entries.toList()
    )

    val jobbAnnonseMinimal = ApiJobbAnnonse(
        arbeidsplassenNoId = UUID_2,
        tittel = "Sykepleier",
        stillingbeskrivelse = null,
        publisert = TIDSPUNKT_2,
        soeknadsfrist = soeknadsfrist(Frist(type = FristType.UKJENT, verdi = null, dato = null)),
        land = "",
        kommune = null,
        sektor = Sektor.Ukjent,
        selskap = "Ukjent",
        tags = emptyList()
    )

    val jobbAnnonseSnarest = jobbAnnonseMinimal.copy(
        arbeidsplassenNoId = UUID_3,
        soeknadsfrist = soeknadsfrist(Frist(type = FristType.SNAREST, verdi = "Snarest", dato = null)),
        sektor = Sektor.Privat
    )

    val jobbAnnonseFortloepende = jobbAnnonseMinimal.copy(
        arbeidsplassenNoId = UUID_4,
        soeknadsfrist = soeknadsfrist(Frist(type = FristType.FORTLOEPENDE, verdi = "Løpende", dato = null)),
        sektor = Sektor.Privat
    )

    val mineStillingerFull = MineStillingerResponse(
        sistKjoert = TIDSPUNKT_2,
        soek = stedSoekFull.api(),
        resultat = listOf(jobbAnnonseFull, jobbAnnonseMinimal, jobbAnnonseSnarest, jobbAnnonseFortloepende)
    )

    val mineStillingerTom = MineStillingerResponse(
        sistKjoert = null,
        soek = stedSoekTomt.api(),
        resultat = emptyList()
    )

    val mineStillingerReisevei = MineStillingerResponse(
        sistKjoert = TIDSPUNKT_1,
        soek = reiseveiSoekFull.api(),
        resultat = emptyList()
    )
}
