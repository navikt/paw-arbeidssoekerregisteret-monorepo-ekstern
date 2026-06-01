package no.naw.paw.minestillinger.domain

import no.naw.paw.minestillinger.api.ApiStedSoek
import no.naw.paw.minestillinger.api.vo.ApiFylke

data class StedSoek(
    override val soekType: StillingssoekType,
    val fylker: List<Fylke>,
    override val soekeord: List<String>,
    val styrk08: List<String>,
    val soekeTags: List<SoekeTag> = emptyList(),
) : Stillingssoek, HarSoekeord

fun stedSoek(
    fylker: List<Fylke>,
    soekeord: List<String>,
    styrk08: List<String>,
    soekeTags: List<SoekeTag>
): StedSoek = StedSoek(
    soekType = StillingssoekType.STED_SOEK_V1,
    fylker = fylker,
    soekeord = soekeord,
    styrk08 = styrk08,
    soekeTags = soekeTags
)

fun ApiStedSoek.domain() = StedSoek(
    soekType = soekType.domain(),
    fylker = fylker.map(ApiFylke::domain),
    soekeord = soekeord,
    styrk08 = styrk08,
    soekeTags = soekeTags.map(::soekeTag)
)

fun StedSoek.api(): ApiStedSoek = ApiStedSoek(
    soekType = soekType.api(),
    fylker = fylker.map(Fylke::api),
    soekeord = soekeord,
    styrk08 = styrk08,
    soekeTags = soekeTags.map(::apiSpoekeTag)
)

