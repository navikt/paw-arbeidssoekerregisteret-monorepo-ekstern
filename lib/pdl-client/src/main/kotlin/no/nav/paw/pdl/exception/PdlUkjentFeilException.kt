package no.nav.paw.pdl.exception

import com.expediagroup.graphql.client.types.GraphQLClientError
import io.ktor.http.HttpStatusCode
import no.nav.paw.pdl.model.pdlErrorType

open class PdlUkjentFeilException(
    override val message: String,
    override val graphQLClientErrors: List<GraphQLClientError>
) : PdlClientException(HttpStatusCode.InternalServerError, pdlErrorType("ukjent-feil"), message, graphQLClientErrors)