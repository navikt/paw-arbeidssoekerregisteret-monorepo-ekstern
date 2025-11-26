package no.nav.paw.arbeidssoekerregisteret.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType
import java.net.URI

val PROFILERING_IKKE_STOETTET_ERROR_TYPE: URI =
    ErrorType.domain("egenvurdering").error("profilering-ikke-støttet").build()

class ProfileringIkkeStoettetException(
    profilering: ProfilertTil,
    egenvurdering: ProfilertTil
) : ServerResponseException(
    status = HttpStatusCode.BadRequest,
    type = PROFILERING_IKKE_STOETTET_ERROR_TYPE,
    message = "Egenvurdering ${egenvurdering.name} er ikke støttet for profilering ${profilering.name}"
)