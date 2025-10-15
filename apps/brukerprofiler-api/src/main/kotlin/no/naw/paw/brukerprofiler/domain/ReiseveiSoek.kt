package no.naw.paw.brukerprofiler.domain

import no.naw.paw.brukerprofiler.api.ApiReiseveiSoek

data class ReiseveiSoek(
    override val soekType: StillingssoekType,
    val maksAvstandKm: Int,
    val postnummer: String,
    override val soekeord: List<String>
): Stillingssoek, HarSoekeord

fun reiseveiSoek(
    maksAvstandKm: Int,
    postnummer: String,
    soekeord: List<String>,
): ReiseveiSoek = ReiseveiSoek(
    soekType = StillingssoekType.REISEVEI_SOEK_V1,
    maksAvstandKm = maksAvstandKm,
    postnummer = postnummer,
    soekeord = soekeord
)

fun ReiseveiSoek.api() = ApiReiseveiSoek(
    soekType = soekType.api(),
    maksAvstandKm = maksAvstandKm,
    postnummer = postnummer,
    soekeord = soekeord
)

fun ApiReiseveiSoek.domain() = ReiseveiSoek(
    soekType = soekType.domain(),
    maksAvstandKm = maksAvstandKm,
    postnummer = postnummer,
    soekeord = soekeord
)