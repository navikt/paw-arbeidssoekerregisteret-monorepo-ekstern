package no.naw.paw.brukerprofiler.api

import com.fasterxml.jackson.annotation.JsonTypeName
import no.naw.paw.brukerprofiler.api.vo.ApiStillingssoekType
import no.naw.paw.brukerprofiler.domain.ReiseveiSoek
import no.naw.paw.brukerprofiler.domain.domain

@JsonTypeName("REISEVEI_SOEK_V1")
data class ApiReiseveiSoek(
    override val soekType: ApiStillingssoekType,
    val maksAvstandKm: Int,
    val postnummer: String,
    override val soekeord: List<String>
): ApiStillingssoek, ApiHarSoekeord

fun ApiReiseveiSoek.domain() = ReiseveiSoek(
    soekType = soekType.domain(),
    maksAvstandKm = maksAvstandKm,
    postnummer = postnummer,
    soekeord = soekeord
)