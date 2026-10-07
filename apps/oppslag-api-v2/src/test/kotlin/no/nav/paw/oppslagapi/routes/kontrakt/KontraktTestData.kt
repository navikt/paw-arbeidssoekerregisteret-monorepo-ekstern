package no.nav.paw.oppslagapi.routes.kontrakt

import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.egenvurdering_v1
import no.nav.paw.oppslagapi.data.opplysninger_om_arbeidssoeker_v4
import no.nav.paw.oppslagapi.data.pa_vegne_av_start_v1
import no.nav.paw.oppslagapi.data.pa_vegne_av_stopp_v1
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.data.profilering_v1
import no.nav.paw.oppslagapi.data.query.genererTidslinje
import no.nav.paw.oppslagapi.model.v2.Annet
import no.nav.paw.oppslagapi.model.v2.AvviksType
import no.nav.paw.oppslagapi.model.v2.Bekreftelse
import no.nav.paw.oppslagapi.model.v2.Bekreftelsesloesning
import no.nav.paw.oppslagapi.model.v2.Beskrivelse
import no.nav.paw.oppslagapi.model.v2.BeskrivelseMedDetaljer
import no.nav.paw.oppslagapi.model.v2.Bruker
import no.nav.paw.oppslagapi.model.v2.BrukerType
import no.nav.paw.oppslagapi.model.v2.Egenvurdering
import no.nav.paw.oppslagapi.model.v2.Helse
import no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke
import no.nav.paw.oppslagapi.model.v2.Jobbsituasjon
import no.nav.paw.oppslagapi.model.v2.Metadata
import no.nav.paw.oppslagapi.model.v2.OpplysningerOmArbeidssoeker
import no.nav.paw.oppslagapi.model.v2.PaaVegneAvStart
import no.nav.paw.oppslagapi.model.v2.PaaVegneAvStopp
import no.nav.paw.oppslagapi.model.v2.Profilering
import no.nav.paw.oppslagapi.model.v2.ProfilertTil
import no.nav.paw.oppslagapi.model.v2.Svar
import no.nav.paw.oppslagapi.model.v2.Tidslinje
import no.nav.paw.oppslagapi.model.v2.TidspunktFraKilde
import no.nav.paw.oppslagapi.model.v2.Utdanning
import no.nav.paw.oppslagapi.test.TestData
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Faste, deterministiske data for JSON-kontrakttestene. Radene har samme form som det
 * DatabaseQuerySupport returnerer i produksjon (Row med v2-modellobjekter).
 *
 * Endres disse verdiene, må fasitfilene genereres på nytt. Det skal ikke skje: fasitfilene
 * låser formatet vi sender til konsumenter.
 *
 * - Periode 1: fullt utfylt og avsluttet. Inneholder alle hendelsestyper og alle bekreftelsesstatuser
 *   (GYLDIG, UVENTET_KILDE, UTENFOR_PERIODE).
 * - Periode 2: aktiv, med valgfrie felter satt til null (sikkerhetsnivaa, tidspunktFraKilde, alder,
 *   utdanning/helse/jobbsituasjon/annet, bestaatt/godkjent, helsetilstand, fristBrutt).
 * - Periode 3: aktiv, bare startet. Gir snapshot der alle valgfrie felter er null.
 */
object KontraktTestData {
    val identitetsnummer: Identitetsnummer = TestData.fnr1
    private val ident = identitetsnummer.value

    val periode1Id: UUID = UUID.fromString("11111111-1111-4111-8111-000000000001")
    val periode2Id: UUID = UUID.fromString("11111111-1111-4111-8111-000000000002")
    val periode3Id: UUID = UUID.fromString("11111111-1111-4111-8111-000000000003")

    private val t0: Instant = Instant.parse("2026-01-15T10:15:30.123456Z")
    private fun t(dager: Long, minutter: Long = 0): Instant =
        t0 + Duration.ofDays(dager) + Duration.ofMinutes(minutter)

    private val sluttbruker = Bruker(type = BrukerType.SLUTTBRUKER, id = ident, sikkerhetsnivaa = "idporten-loa-high")
    private val sluttbrukerUtenSikkerhetsnivaa = Bruker(type = BrukerType.SLUTTBRUKER, id = ident)
    private val veileder = Bruker(type = BrukerType.VEILEDER, id = "Z20001", sikkerhetsnivaa = "azure:Level4")
    private val system = Bruker(type = BrukerType.SYSTEM, id = "paw-arbeidssoekerregisteret-profilering")
    private val udefinert = Bruker(type = BrukerType.UDEFINERT, id = "ukjent-kilde")

    private fun metadata(
        tidspunkt: Instant,
        utfoertAv: Bruker,
        kilde: String = "paw-arbeidssoekerregisteret-api-inngang",
        aarsak: String = "Kontrakttest",
        tidspunktFraKilde: TidspunktFraKilde? = null
    ) = Metadata(
        tidspunkt = tidspunkt,
        utfoertAv = utfoertAv,
        kilde = kilde,
        aarsak = aarsak,
        tidspunktFraKilde = tidspunktFraKilde
    )

    private fun <A : Any> rad(type: String, periodeId: UUID, tidspunkt: Instant, data: A): Row<Any> = Row(
        type = type,
        identitetsnummer = ident,
        periodeId = periodeId,
        timestamp = tidspunkt,
        data = data
    )

    private fun bekreftelse(
        id: String,
        periodeId: UUID,
        loesning: Bekreftelsesloesning,
        tidspunkt: Instant,
        utfoertAv: Bruker
    ) = Bekreftelse(
        periodeId = periodeId,
        bekreftelsesloesning = loesning,
        id = UUID.fromString(id),
        svar = Svar(
            sendtInnAv = metadata(tidspunkt = tidspunkt, utfoertAv = utfoertAv, kilde = "paw-bekreftelse"),
            gjelderFra = tidspunkt - Duration.ofDays(14),
            gjelderTil = tidspunkt,
            harJobbetIDennePerioden = true,
            vilFortsetteSomArbeidssoeker = false
        )
    )

    // ---------- Periode 1: fullt utfylt, avsluttet ----------
    private val p1Opplysninger = OpplysningerOmArbeidssoeker(
        id = UUID.fromString("22222222-2222-4222-8222-000000000001"),
        periodeId = periode1Id,
        sendtInnAv = metadata(t(0, 1), veileder),
        utdanning = Utdanning(nus = "3", bestaatt = JaNeiVetIkke.JA, godkjent = JaNeiVetIkke.NEI),
        helse = Helse(helsetilstandHindrerArbeid = JaNeiVetIkke.NEI),
        jobbsituasjon = Jobbsituasjon(
            beskrivelser = listOf(
                BeskrivelseMedDetaljer(
                    beskrivelse = Beskrivelse.HAR_SAGT_OPP,
                    detaljer = linkedMapOf(
                        "gjelder_fra_dato_iso8601" to "2025-12-01",
                        "stilling_styrk08" to "2359",
                        "stilling" to "Lærer"
                    )
                ),
                BeskrivelseMedDetaljer(
                    beskrivelse = Beskrivelse.DELTIDSJOBB_VIL_MER,
                    detaljer = linkedMapOf("prosent" to "50")
                )
            )
        ),
        annet = Annet(andreForholdHindrerArbeid = JaNeiVetIkke.VET_IKKE)
    )
    private val p1Profilering = Profilering(
        id = UUID.fromString("33333333-3333-4333-8333-000000000001"),
        periodeId = periode1Id,
        opplysningerOmArbeidssokerId = p1Opplysninger.id,
        sendtInnAv = metadata(t(0, 2), system, kilde = "paw-arbeidssoekerregisteret-profilering"),
        profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
        jobbetSammenhengendeSeksAvTolvSisteMnd = true,
        alder = 42
    )
    private val p1Egenvurdering = Egenvurdering(
        id = UUID.fromString("44444444-4444-4444-8444-000000000001"),
        periodeId = periode1Id,
        profileringId = p1Profilering.id,
        sendtInnAv = metadata(t(1), sluttbruker, kilde = "paw-arbeidssoeker-egenvurdering"),
        profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
        egenvurdering = ProfilertTil.ANTATT_GODE_MULIGHETER
    )

    private val raderPeriode1: List<Row<Any>> = listOf(
        rad(
            periode_startet_v1, periode1Id, t(0), metadata(
                tidspunkt = t(0),
                utfoertAv = sluttbruker,
                aarsak = "Er over 18 år, er bosatt i Norge",
                tidspunktFraKilde = TidspunktFraKilde(
                    tidspunkt = Instant.parse("2026-01-15T10:14:00.654321Z"),
                    avviksType = AvviksType.FORSINKELSE
                )
            )
        ),
        rad(opplysninger_om_arbeidssoeker_v4, periode1Id, t(0, 1), p1Opplysninger),
        rad(profilering_v1, periode1Id, t(0, 2), p1Profilering),
        rad(egenvurdering_v1, periode1Id, t(1), p1Egenvurdering),
        rad(
            pa_vegne_av_start_v1, periode1Id, t(2), PaaVegneAvStart(
                periodeId = periode1Id,
                bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER,
                intervalMS = Duration.ofDays(14).toMillis(),
                graceMS = Duration.ofDays(7).toMillis()
            )
        ),
        // GYLDIG: kommer fra ansvarlig løsning
        rad(
            bekreftelsemelding_v1, periode1Id, t(14), bekreftelse(
                "55555555-5555-4555-8555-000000000001", periode1Id, Bekreftelsesloesning.DAGPENGER, t(14), system
            )
        ),
        // UVENTET_KILDE: kommer fra en løsning som ikke er ansvarlig
        rad(
            bekreftelsemelding_v1, periode1Id, t(15), bekreftelse(
                "55555555-5555-4555-8555-000000000002", periode1Id,
                Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING, t(15), veileder
            )
        ),
        rad(
            pa_vegne_av_stopp_v1, periode1Id, t(20), PaaVegneAvStopp(
                periodeId = periode1Id,
                bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER,
                fristBrutt = true
            )
        ),
        rad(
            periode_avsluttet_v1, periode1Id, t(30), metadata(
                tidspunkt = t(30),
                utfoertAv = veileder,
                kilde = "paw-arbeidssoekerregisteret-api-inngang",
                aarsak = "Stopp av periode",
                tidspunktFraKilde = TidspunktFraKilde(
                    tidspunkt = t(29),
                    avviksType = AvviksType.TIDSPUNKT_KORRIGERT
                )
            )
        ),
        // UTENFOR_PERIODE: mottatt etter at perioden ble avsluttet
        rad(
            bekreftelsemelding_v1, periode1Id, t(31), bekreftelse(
                "55555555-5555-4555-8555-000000000003", periode1Id,
                Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET, t(31), sluttbruker
            )
        )
    )

    // ---------- Periode 2: aktiv, valgfrie felter er null ----------
    private val p2OpplysningerUtenInnhold = OpplysningerOmArbeidssoeker(
        id = UUID.fromString("22222222-2222-4222-8222-000000000002"),
        periodeId = periode2Id,
        sendtInnAv = metadata(t(60, 1), sluttbrukerUtenSikkerhetsnivaa),
        utdanning = null,
        helse = null,
        jobbsituasjon = null,
        annet = null
    )
    private val p2OpplysningerMedNullfelter = OpplysningerOmArbeidssoeker(
        id = UUID.fromString("22222222-2222-4222-8222-000000000003"),
        periodeId = periode2Id,
        sendtInnAv = metadata(t(60, 5), sluttbrukerUtenSikkerhetsnivaa),
        utdanning = Utdanning(nus = "0", bestaatt = null, godkjent = null),
        helse = Helse(helsetilstandHindrerArbeid = null),
        jobbsituasjon = Jobbsituasjon(beskrivelser = emptyList()),
        annet = Annet(andreForholdHindrerArbeid = JaNeiVetIkke.UKJENT_VERDI)
    )
    private val p2Profilering = Profilering(
        id = UUID.fromString("33333333-3333-4333-8333-000000000002"),
        periodeId = periode2Id,
        opplysningerOmArbeidssokerId = p2OpplysningerMedNullfelter.id,
        sendtInnAv = metadata(t(60, 6), system, kilde = "paw-arbeidssoekerregisteret-profilering"),
        profilertTil = ProfilertTil.OPPGITT_HINDRINGER,
        jobbetSammenhengendeSeksAvTolvSisteMnd = false,
        alder = null
    )

    private val raderPeriode2: List<Row<Any>> = listOf(
        rad(periode_startet_v1, periode2Id, t(60), metadata(t(60), udefinert)),
        rad(opplysninger_om_arbeidssoeker_v4, periode2Id, t(60, 1), p2OpplysningerUtenInnhold),
        rad(opplysninger_om_arbeidssoeker_v4, periode2Id, t(60, 5), p2OpplysningerMedNullfelter),
        rad(profilering_v1, periode2Id, t(60, 6), p2Profilering),
        rad(
            pa_vegne_av_start_v1, periode2Id, t(61), PaaVegneAvStart(
                periodeId = periode2Id,
                bekreftelsesloesning = Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING,
                intervalMS = Duration.ofDays(14).toMillis(),
                graceMS = Duration.ofDays(7).toMillis()
            )
        ),
        rad(
            pa_vegne_av_stopp_v1, periode2Id, t(62), PaaVegneAvStopp(
                periodeId = periode2Id,
                bekreftelsesloesning = Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING,
                fristBrutt = null
            )
        ),
        rad(
            bekreftelsemelding_v1, periode2Id, t(75), bekreftelse(
                "55555555-5555-4555-8555-000000000004", periode2Id,
                Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET, t(75), sluttbrukerUtenSikkerhetsnivaa
            )
        )
    )

    // ---------- Periode 3: aktiv, bare startet ----------
    private val raderPeriode3: List<Row<Any>> = listOf(
        rad(periode_startet_v1, periode3Id, t(90), metadata(t(90), sluttbruker))
    )

    val raderPerPeriode: List<Pair<UUID, List<Row<Any>>>> = listOf(
        periode1Id to raderPeriode1,
        periode2Id to raderPeriode2,
        periode3Id to raderPeriode3
    )

    /** Standard for liste-endepunktene: én avsluttet, fullt utfylt periode og én aktiv periode med null-felter. */
    val standardPerioder: List<UUID> = listOf(periode1Id, periode2Id)

    /** Tidslinjene slik produksjonskoden genererer dem fra radene. */
    fun tidslinjer(perioder: List<UUID> = standardPerioder): List<Tidslinje> =
        genererTidslinje(raderPerPeriode.filter { (id, _) -> id in perioder })
}
