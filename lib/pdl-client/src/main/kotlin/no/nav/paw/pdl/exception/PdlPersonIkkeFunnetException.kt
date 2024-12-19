package no.nav.paw.pdl.exception

import com.expediagroup.graphql.client.types.GraphQLClientError
import io.ktor.http.HttpStatusCode
import no.nav.paw.pdl.model.pdlErrorType

class PdlPersonIkkeFunnetException(
    override val message: String,
    override val graphQLClientErrors: List<GraphQLClientError>
) : PdlClientException(HttpStatusCode.BadRequest, pdlErrorType("person-ikke-funnet"), message, graphQLClientErrors)