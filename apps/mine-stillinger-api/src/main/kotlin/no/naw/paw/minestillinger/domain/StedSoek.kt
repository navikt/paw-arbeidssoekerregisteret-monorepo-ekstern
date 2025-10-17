package no.naw.paw.minestillinger.domain

import no.naw.paw.minestillinger.api.ApiStedSoek
import no.naw.paw.minestillinger.api.vo.ApiFylke

data class StedSoek(
    override val soekType: StillingssoekType,
    val fylker: List<Fylke>,
    override val soekeord: List<String>,
    val styrk08: List<String>
) : Stillingssoek, HarSoekeord

fun stedSoek(
    fylker: List<Fylke>,
    soekeord: List<String>,
    styrk08: List<String>
): StedSoek = StedSoek(
    soekType = StillingssoekType.STED_SOEK_V1,
    fylker = fylker,
    soekeord = soekeord,
    styrk08 = styrk08
)

fun ApiStedSoek.domain() = StedSoek(
    soekType = soekType.domain(),
    fylker = fylker.map(ApiFylke::domain),
    soekeord = soekeord,
    styrk08 = styrk08Kode
)

fun StedSoek.api(): ApiStedSoek = ApiStedSoek(
    soekType = soekType.api(),
    fylker = fylker.map(Fylke::api),
    soekeord = soekeord,
    styrk08Kode = styrk08
)

