package no.nav.paw.oppslagapi.data

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.model.Identitetsnummer
import java.time.Instant
import java.util.UUID

suspend fun KafkaKeysClient.finnAlleIdenterForPerson(identitetsnummer: Identitetsnummer): Response<List<Identitetsnummer>> {
    return runCatching { getInfo(identitetsnummer.verdi) }
        .map {
            it?.info?.pdlData?.id
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