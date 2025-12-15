package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.VeilarbdialogClientConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.Annen409Feil
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.ArbeidsoppfølgingsperiodeAvsluttet
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.BrukerKanIkkeVarsles
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.DialogRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.DialogResponse
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.DialogResultat
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType
import no.nav.paw.security.texas.TexasClient
import java.net.URI

//https://veilarbdialog.intern.dev.nav.no/veilarbdialog/swagger-ui/index.html#/Dialog(tr%C3%A5d)%20og%20meldings%20API/nyMelding

const val veilarbDialogPath = "/api/dialog"

class VeilarbdialogClient(
    private val config: VeilarbdialogClientConfig,
    private val texasClient: TexasClient,
    private val httpClient: HttpClient = createHttpClient(),
) {
    suspend fun lagEllerOppdaterDialog(dialogRequest: DialogRequest): DialogResultat {
        val response = httpClient.post(config.url + veilarbDialogPath) {
            contentType(ContentType.Application.Json)
            bearerAuth(texasClient.getMachineToMachineToken(config.target).accessToken)
            setBody(dialogRequest)
        }
        return when {
            response.status.isSuccess() -> response.body<DialogResponse>()
            response.status == Conflict -> response.handleConflict()
            else -> {
                throw VeilarbdialogClientException(
                    status = response.status,
                    type = ErrorType.domain("egenvurdering").error("ukjent-veilarbdialog-feil").build(),
                    message = "Status: ${response.status}. Body: ${response.bodyAsText()}"
                )
            }
        }
    }

    private suspend fun HttpResponse.handleConflict(): DialogResultat {
        val body = bodyAsText()
        return when {
            body.contains("Kan ikke sende henvendelse på historisk dialog") -> ArbeidsoppfølgingsperiodeAvsluttet
            body.contains("Bruker kan ikke varsles") -> BrukerKanIkkeVarsles
            else -> Annen409Feil(body)
        }
    }
}

class VeilarbdialogClientException(
    override val status: HttpStatusCode,
    override val type: URI,
    override val message: String,
) : ClientResponseException(status, type, message)

