package no.nav.paw.serialization.kontrakt

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.time.Instant

enum class Eksempelkategori { FOERSTE, ANDRE }

data class NestetEksempel(
    val navn: String,
    val antall: Int,
)

/** Representativ dataklasse som brukes til å låse oppførselen til de delte ObjectMapper-oppsettene. */
data class MapperEksempel(
    val id: UUID,
    val tidspunkt: Instant,
    val dato: LocalDate,
    val datoTid: LocalDateTime,
    val varighet: Duration,
    val valgfriTekst: String?,
    val tomListe: List<String>,
    val kategori: Eksempelkategori,
    val nestet: NestetEksempel,
    val nestetListe: List<NestetEksempel>,
    val stortTall: Long,
    val flagg: Boolean,
)

val MAPPER_EKSEMPEL = MapperEksempel(
    id = FAST_ID,
    tidspunkt = FAST_TIDSPUNKT,
    dato = LocalDate.of(2026, 1, 15),
    datoTid = LocalDateTime.of(2026, 1, 15, 10, 15, 30, 123_456_000),
    varighet = Duration.ofHours(1).plusMinutes(30).plusMillis(250),
    valgfriTekst = null,
    tomListe = emptyList(),
    kategori = Eksempelkategori.ANDRE,
    nestet = NestetEksempel(navn = "nestet", antall = 3),
    nestetListe = listOf(NestetEksempel("a", 1), NestetEksempel("b", 2)),
    stortTall = 9_007_199_254_740_993L,
    flagg = true,
)

/** Brukes for å dokumentere hvordan null/manglende verdier for ikke-nullbare felt behandles i dag. */
data class MedListe(val liste: List<String>)
data class MedMap(val kart: Map<String, String>)
data class MedStandardverdi(val verdi: String = "standard")
