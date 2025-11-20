package no.nav.paw.oppslagapi.model.v3

sealed interface QueryRequest {
    val type: QueryType
}