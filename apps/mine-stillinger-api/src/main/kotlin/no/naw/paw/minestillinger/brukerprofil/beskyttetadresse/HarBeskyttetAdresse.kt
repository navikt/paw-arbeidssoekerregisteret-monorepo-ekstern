package no.naw.paw.minestillinger.brukerprofil.beskyttetadresse

import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.pdl.client.hentAdressebeskyttelse
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.enums.AdressebeskyttelseGradering

const val BEHANDLINGSNUMMER = "B452"
private val logger = buildApplicationLogger

suspend fun PdlClient.harBeskyttetAdresse(identitetsnummer: Identitetsnummer): Boolean {
    try {
        val adressebeskyttelse = hentAdressebeskyttelse(identitetsnummer.value, null, BEHANDLINGSNUMMER) ?: emptyList()
        Span.current().addEvent(
            "adressebeskyttelse", Attributes.of(
                longKey("antall"), adressebeskyttelse.size.toLong()
            )
        )
        if (adressebeskyttelse.size > 1) {
            logger.warn("Person har flere adressebeskyttelser")
        }
        return adressebeskyttelse.any { it.gradering != AdressebeskyttelseGradering.UGRADERT }
    } catch (e: PdlUkjentFeilException) {
        logger.error("Feil ved henting av adressebeskyttelse: ${e.graphQLClientErrors}", e)
        throw e
    }
}

suspend fun PdlClient.harBeskyttetAdresseBulk(identitetsnummer: List<Identitetsnummer>): List<AdressebeskyttelseResultat> {
    try {
        return hentAdressebeskyttelse(identitetsnummer.map { it.value }, null, BEHANDLINGSNUMMER)
            ?.map { personResultat ->
                val ident = Identitetsnummer(personResultat.ident)
                val feilkode = personResultat.code.takeUnless { it.equals("ok", ignoreCase = true) }
                if (feilkode != null) {
                    AdressebeskyttelseFeil(ident, feilkode)
                } else {
                    val harBeskyttetAdresse = personResultat.person
                        ?.adressebeskyttelse
                        ?.any { it.gradering != AdressebeskyttelseGradering.UGRADERT }
                        ?: false
                    AdressebeskyttelseVerdi(
                        identitetsnummer = ident,
                        harBeskyttetAdresse = harBeskyttetAdresse
                    )
                }
            } ?: emptyList()
    } catch (e: PdlUkjentFeilException) {
        logger.error("Feil ved henting av adressebeskyttelse: ${e.graphQLClientErrors}", e)
        throw e
    }
}

sealed interface AdressebeskyttelseResultat {
    val identitetsnummer: Identitetsnummer
}

data class AdressebeskyttelseVerdi(override val identitetsnummer: Identitetsnummer, val harBeskyttetAdresse: Boolean) :
    AdressebeskyttelseResultat

data class AdressebeskyttelseFeil(override val identitetsnummer: Identitetsnummer, val code: String) :
    AdressebeskyttelseResultat

