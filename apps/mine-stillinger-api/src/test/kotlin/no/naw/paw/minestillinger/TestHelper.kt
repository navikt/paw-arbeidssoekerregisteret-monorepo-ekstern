package no.naw.paw.minestillinger

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.naw.paw.minestillinger.api.ApiStedSoek
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import no.naw.paw.minestillinger.route.BRUKERPROFIL_PATH

// Helper class to reduce repetition in HTTP calls
class TestHelper(private val oauthServer: MockOAuth2Server) {
    suspend fun HttpClient.getBrukerprofil(
        identitetsnummer: Identitetsnummer
    ) = get(BRUKERPROFIL_PATH) {
        bearerAuth(oauthServer.sluttbrukerToken(id = identitetsnummer))
        contentType(ContentType.Application.Json)
    }.validateAgainstOpenApiSpec()

    suspend fun HttpClient.setTjenestestatus(
        status: ApiTjenesteStatus,
        identitetsnummer: Identitetsnummer
    ) = put("${BRUKERPROFIL_PATH}/tjenestestatus/${status.name}") {
        bearerAuth(oauthServer.sluttbrukerToken(id = identitetsnummer))
        contentType(ContentType.Application.Json)
    }.validateAgainstOpenApiSpec()

    suspend fun HttpClient.lagreStillingssoek(
        søk: List<ApiStedSoek>,
        identitetsnummer: Identitetsnummer
    ) = put("${BRUKERPROFIL_PATH}/stillingssoek") {
        bearerAuth(oauthServer.sluttbrukerToken(id = identitetsnummer))
        contentType(ContentType.Application.Json)
        setBody(søk)
    }.validateAgainstOpenApiSpec()

    suspend fun HttpClient.mineLedigeStillinger(
        identitetsnummer: Identitetsnummer
    ) = get("/api/v1/ledigestillinger") {
        bearerAuth(oauthServer.sluttbrukerToken(id = identitetsnummer))
        contentType(ContentType.Application.Json)
    }.validateAgainstOpenApiSpec()
}