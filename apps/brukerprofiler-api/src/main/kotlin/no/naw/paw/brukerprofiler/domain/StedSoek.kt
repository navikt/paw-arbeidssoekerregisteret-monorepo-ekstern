package no.naw.paw.brukerprofiler.domain

import no.naw.paw.brukerprofiler.api.ApiStedSoek
import no.naw.paw.brukerprofiler.api.vo.ApiFylke

data class StedSoek(
    override val soekType: StillingssoekType,
    val fylker: List<Fylke>,
    override val soekeord: List<String>,
) : Stillingssoek, HarSoekeord

fun stedSoek(
    fylker: List<Fylke>,
    soekeord: List<String>,
): StedSoek = StedSoek(
    soekType = StillingssoekType.STED_SOEK_V1,
    fylker = fylker,
    soekeord = soekeord
)

fun ApiStedSoek.domain() = StedSoek(
    soekType = soekType.domain(),
    fylker = fylker.map(ApiFylke::domain),
    soekeord = soekeord
)

fun StedSoek.api(): ApiStedSoek = ApiStedSoek(
    soekType = soekType.api(),
    fylker = fylker.map(Fylke::api),
    soekeord = soekeord
)

