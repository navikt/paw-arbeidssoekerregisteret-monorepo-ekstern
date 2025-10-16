package no.naw.paw.brukerprofiler.api

import com.fasterxml.jackson.annotation.JsonTypeName
import no.naw.paw.brukerprofiler.api.vo.ApiFylke
import no.naw.paw.brukerprofiler.api.vo.ApiStillingssoekType
import no.naw.paw.brukerprofiler.domain.StedSoek
import no.naw.paw.brukerprofiler.domain.domain

@JsonTypeName("STED_SOEK_V1")
data class ApiStedSoek(
    override val soekType: ApiStillingssoekType,
    val fylker: List<ApiFylke>,
    override val soekeord: List<String>,
): ApiStillingssoek, ApiHarSoekeord

fun ApiStedSoek.domain() = StedSoek(
    soekType = soekType.domain(),
    fylker = fylker.map { it.domain() },
    soekeord = soekeord
)