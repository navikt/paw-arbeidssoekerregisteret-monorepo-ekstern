package no.naw.paw.minestillinger.api

import com.fasterxml.jackson.annotation.JsonTypeName
import no.naw.paw.minestillinger.api.vo.ApiFylke
import no.naw.paw.minestillinger.api.vo.ApiStillingssoekType
import no.naw.paw.minestillinger.domain.StedSoek
import no.naw.paw.minestillinger.domain.domain

@JsonTypeName("STED_SOEK_V1")
data class ApiStedSoek(
    override val soekType: ApiStillingssoekType,
    val fylker: List<ApiFylke>,
    override val soekeord: List<String>,
    val styrk08: List<String>
) : ApiStillingssoek, ApiHarSoekeord

fun ApiStedSoek.domain() = StedSoek(
    soekType = soekType.domain(),
    fylker = fylker.map { it.domain() },
    soekeord = soekeord,
    styrk08 = styrk08
)


