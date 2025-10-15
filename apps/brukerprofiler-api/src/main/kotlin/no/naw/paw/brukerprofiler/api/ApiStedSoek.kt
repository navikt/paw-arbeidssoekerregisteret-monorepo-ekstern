package no.naw.paw.brukerprofiler.api

import com.fasterxml.jackson.annotation.JsonTypeName
import no.naw.paw.brukerprofiler.api.vo.ApiFylke
import no.naw.paw.brukerprofiler.api.vo.ApiStillingssoekType

@JsonTypeName("STED_SOEK_V1")
data class ApiStedSoek(
    override val soekType: ApiStillingssoekType,
    val fylker: List<ApiFylke>,
    override val soekeord: List<String>,
): ApiStillingssoek, ApiHarSoekeord