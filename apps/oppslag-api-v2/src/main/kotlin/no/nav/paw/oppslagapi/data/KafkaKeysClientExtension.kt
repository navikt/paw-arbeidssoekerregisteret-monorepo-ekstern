package no.nav.paw.oppslagapi.data

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.model.Identitet
import no.nav.paw.kafkakeygenerator.model.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentiteterResponse
import no.nav.paw.oppslagapi.exception.FinnIdentiteterFeiletException
import java.time.Instant
import java.util.*

suspend fun KafkaKeysClient.finnAlleIdenterForPerson(identitetsnummer: Identitetsnummer): Response<List<Identitetsnummer>> {
    return runCatching { getInfo(identitetsnummer.value) }
        .map { response ->
            response?.info?.pdlData?.id
                ?.filter { it.gruppe.equals("FOLKEREGISTERIDENT", ignoreCase = true) }
                ?: emptyList()
        }.map { folkeregisterIdenter -> folkeregisterIdenter.map { Identitetsnummer(it.id) } }
        .map { Data(it) }
        .recover { throwable ->
            ProblemDetails(
                id = UUID.randomUUID(),
                type = ErrorType
                    .domain("kafka-keys")
                    .error("finn_alle_identer_for_person")
                    .build(),
                status = HttpStatusCode.ServiceUnavailable,
                title = "Feil ved henting av identer",
                detail = "Kunne ikke hente identer for person: ${throwable.message}",
                instance = "/kafka-keys/finn-alle-identifier-for-person",
                timestamp = Instant.now()
            )
        }.getOrThrow()
}

suspend fun KafkaKeysClient.finnFolkeregisteridenterKonfliktAware(
    identitetsnummer: Identitetsnummer
): List<Identitetsnummer> {
    return runCatching { getIdentiteter(identitetsnummer.value) }
        .map { response -> response.utledFolkeregisteridenterKonfliktAware() }
        .fold(
            onSuccess = { identiteter ->
                identiteter.map { Identitetsnummer(it.identitet) }
            },
            onFailure = { throwable ->
                throw FinnIdentiteterFeiletException("Feil ved henting av identiteter", throwable)
            })
}

private fun IdentiteterResponse.utledFolkeregisteridenterKonfliktAware(): List<Identitet> {
    return if (this.harKonflikter()) {
        this.pdlIdentiteter ?: this.identiteter
    } else {
        this.identiteter
    }.filter { it.type == IdentitetType.FOLKEREGISTERIDENT }
}

private fun IdentiteterResponse.harKonflikter(): Boolean {
    return this.konflikter?.isNotEmpty() ?: false
}