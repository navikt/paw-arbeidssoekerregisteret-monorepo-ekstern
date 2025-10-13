package no.naw.paw.brukerprofiler

import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.pdl.client.hentAdressebeskyttelse
import no.nav.paw.pdl.graphql.generated.enums.AdressebeskyttelseGradering

const val BEHANDLINGSNUMMER = "B452"

suspend fun PdlClient.harBeskyttetAdresse(identitetsnummer: Identitetsnummer): Boolean {
    val adressebeskyttelse = hentAdressebeskyttelse(identitetsnummer.verdi, null, BEHANDLINGSNUMMER) ?: emptyList()
    Span.current().addEvent("adressebeskyttelse", Attributes.of(
        longKey("antall"), adressebeskyttelse.size.toLong())
    )
    if (adressebeskyttelse.size > 1) {
        buildApplicationLogger.warn("Person har flere adressebeskyttelser")
    }
    return adressebeskyttelse.isEmpty() ||
            adressebeskyttelse.all { it.gradering == AdressebeskyttelseGradering.UGRADERT }
}

suspend fun PdlClient.hasBeskyttetAdresse(identitetsnummer: List<Identitetsnummer>): List<AdressebeskyttelseResultat> =
    hentAdressebeskyttelse(identitetsnummer.map { it.verdi }, null, BEHANDLINGSNUMMER)
        ?.map { personResultat ->
            val ident = Identitetsnummer(personResultat.ident)
            val feilkode = personResultat.code.takeIf { it.equals("ok", ignoreCase = true) }
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

sealed interface AdressebeskyttelseResultat {
    val identitetsnummer: Identitetsnummer
}

data class AdressebeskyttelseVerdi(override val identitetsnummer: Identitetsnummer, val harBeskyttetAdresse: Boolean): AdressebeskyttelseResultat
data class AdressebeskyttelseFeil(override val identitetsnummer: Identitetsnummer, val code: String): AdressebeskyttelseResultat

