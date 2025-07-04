package no.nav.paw.arbeidssoekerregisteret

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.paw.arbeidssoekerregisteret.config.VeilarbdialogClientConfig
import no.nav.paw.client.factory.createHttpClient

//https://veilarbdialog.intern.dev.nav.no/veilarbdialog/swagger-ui/index.html#/Dialog(tr%C3%A5d)%20og%20meldings%20API/nyMelding

const val veilarbDialogPostPath = "/api/dialog"

class VeilarbdialogClient(
    private val config: VeilarbdialogClientConfig,
    private val httpClient: HttpClient = createHttpClient(),
) {
    suspend fun lagEllerOppdaterDialog(dialogRequest: DialogRequest): DialogResponse {
        val response = httpClient.post(config.url + veilarbDialogPostPath) {
            contentType(ContentType.Application.Json)
            setBody(dialogRequest)
        }
        if(response.status.isSuccess()) {
            return response.body<DialogResponse>()
        } else {
            throw VeilarbdialogClientException("Feil ved POST av melding til veilarbdialog for egenvurdering.")
        }
    }
}

class VeilarbdialogClientException(message: String) : RuntimeException(message)

data class DialogRequest(
    val tekst: String,
    val dialogId: String? = null,
    val overskrift: String,
    val aktivitetId: String, // TODO: er denne optional?
    val venterPaaSvarFraNav: Boolean,
    val venterPaaSvarFraBruker: Boolean,
    val egenskaper: List<String>, // TODO: er denne optional?
    val fnr: String
)

data class DialogResponse(
    @JsonProperty("id")
    val dialogId: String
)
