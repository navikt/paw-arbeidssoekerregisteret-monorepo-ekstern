package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.VeilarbdialogClientConfig
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType
import java.net.URI

//https://veilarbdialog.intern.dev.nav.no/veilarbdialog/swagger-ui/index.html#/Dialog(tr%C3%A5d)%20og%20meldings%20API/nyMelding

const val veilarbDialogPath = "/api/dialog"

class VeilarbdialogClient(
    private val config: VeilarbdialogClientConfig,
    private val httpClient: HttpClient = createHttpClient(),
) {
    suspend fun lagEllerOppdaterDialog(dialogRequest: DialogRequest): DialogResultat {
        val response = httpClient.post(config.url + veilarbDialogPath) {
            contentType(ContentType.Application.Json)
            setBody(dialogRequest)
        }
        return when {
            response.status.isSuccess() -> response.body<DialogResponse>()
            response.arbeidsoppfølgingsperiodeErAvsluttet() -> ArbeidsoppfølgingsperiodeAvsluttet
            else -> {
                throw VeilarbdialogClientException(
                    status = response.status,
                    type = ErrorType.domain("egenvurdering").error("ukjent-veilarbdialog-feil").build(),
                    message = "Status: ${response.status}. Body: ${response.bodyAsText()}"
                )
            }
        }
    }

    private suspend fun HttpResponse.arbeidsoppfølgingsperiodeErAvsluttet(): Boolean =
        this.status == HttpStatusCode.Conflict && this.bodyAsText()
            //Hacky, men veilarbdialog har ikke en egen feilkode for dette
            .contains("Kan ikke sende henvendelse på historisk dialog")
}

class VeilarbdialogClientException(
    override val status: HttpStatusCode,
    override val type: URI,
    override val message: String,
) : ClientResponseException(status, type, message)

