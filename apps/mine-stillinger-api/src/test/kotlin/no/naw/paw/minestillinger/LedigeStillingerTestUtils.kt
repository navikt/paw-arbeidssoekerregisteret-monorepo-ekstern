package no.naw.paw.minestillinger

import no.naw.paw.ledigestillinger.model.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

fun createFinnStillingerByEgenskaperRequest(
    soekeord: List<String> = emptyList(),
    kategorier: List<String> = emptyList(),
    fylkenummer: String? = null,
    kommunenummer: String? = null,
    paging: Paging = Paging(
        page = 1,
        pageSize = 10,
        sortOrder = SortOrder.DESC
    )
): FinnStillingerByEgenskaperRequest {
    val fylker = fylkenummer?.let {
        listOf(
            Fylke(
                fylkesnummer = it,
                kommuner = kommunenummer
                    ?.let { kn -> listOf(Kommune(kn)) }
                    ?: emptyList()
            ))
    } ?: emptyList()

    return FinnStillingerByEgenskaperRequest(
        type = FinnStillingerType.BY_EGENSKAPER,
        soekeord = soekeord,
        styrkkoder = kategorier,
        fylker = fylker,
        paging = paging
    )
}

fun lagStilling(request: FinnStillingerByEgenskaperRequest): Stilling {
    val now = Instant.now()
    val firstKategori = request.styrkkoder.firstOrNull()

    return Stilling(
        uuid = UUID.randomUUID(),
        adnr = "test-adnr-123",
        tittel = request.soekeord.firstOrNull() ?: "Teststilling",
        status = StillingStatus.AKTIV,
        visning = VisningGrad.UBEGRENSET,
        arbeidsgivernavn = "Test AS",
        arbeidsgiver = Arbeidsgiver(
            orgForm = "AS",
            navn = "Test AS",
            offentligNavn = "Test AS",
            orgNr = "123456789",
            parentOrgNr = null
        ),
        stillingstittel = request.soekeord.firstOrNull() ?: "Teststilling",
        ansettelsesform = "Fast",
        stillingsprosent = Stillingsprosent.HELTID,
        stillingsantall = 1,
        sektor = Sektor.PRIVAT,
        soeknadsfrist = Frist(
            type = FristType.DATO,
            verdi = null,
            dato = LocalDate.now().plusMonths(1)
        ),
        oppstartsfrist = Frist(
            type = FristType.SNAREST,
            verdi = "Snarest",
            dato = null
        ),
        publisert = now,
        utloeper = now.plusSeconds(30 * 24 * 60 * 60), // 30 days
        styrkkoder = firstKategori?.let {
            listOf(StyrkKode(kode = it, navn = "Testkategori $it"))
        } ?: emptyList(),
        lokasjoner = emptyList()
    )
}
