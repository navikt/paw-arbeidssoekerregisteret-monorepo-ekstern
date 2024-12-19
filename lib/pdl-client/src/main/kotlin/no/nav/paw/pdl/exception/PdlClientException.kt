package no.nav.paw.pdl.exception

import com.expediagroup.graphql.client.types.GraphQLClientError
import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ClientResponseException
import java.net.URI

open class PdlClientException(
    override val status: HttpStatusCode,
    override val type: URI,
    override val message: String,
    open val graphQLClientErrors: List<GraphQLClientError>
) : ClientResponseException(status, type, message)