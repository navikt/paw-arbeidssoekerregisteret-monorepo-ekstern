package no.naw.paw.brukerprofiler

import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.pdl.client.hentAdressebeskyttelse
import no.nav.paw.pdl.graphql.generated.enums.AdressebeskyttelseGradering

suspend fun PdlClient.harBeskyttetAdresse(identitetsnummer: Identitetsnummer): Boolean {
    val adressebeskyttelse = hentAdressebeskyttelse(identitetsnummer.verdi, null, "B452") ?: emptyList()
    Span.current().addEvent("adressebeskyttelse", Attributes.of(
        longKey("antall"), adressebeskyttelse.size.toLong())
    )
    if (adressebeskyttelse.size > 1) {
        buildApplicationLogger.warn("Person har flere adressebeskyttelser")
    }
    return adressebeskyttelse.isEmpty() ||
            adressebeskyttelse.all { it.gradering == AdressebeskyttelseGradering.UGRADERT }
}