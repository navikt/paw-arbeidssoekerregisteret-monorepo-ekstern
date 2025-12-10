package no.naw.paw.minestillinger.vedtak14a

import no.nav.paw.felles.model.AktorId
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.hwm.Message
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun lagre14aResultat(
    idFunction: (AktorId) -> Identitetsnummer?,
    brukerprofilTjeneste: BrukerprofilTjeneste,
    source: Sequence<Message<String, Siste14aVedtakMelding>>
) {
    transaction {
        source
            .onEach { message ->
                appLogger.trace("Mottatt melding: topic=${message.topic} - partition=${message.partition} - offset=${message.offset}")
            }
            .mapNotNull { message ->
                val brukerprofil = message.value.aktorId
                    ?.let { AktorId(it.value) }
                    ?.let(idFunction)
                    ?.let(brukerprofilTjeneste::hentLokalBrukerProfilEllerNull)
                val innsatsgruppe = message.value.innsatsgruppe
                if (brukerprofil != null && innsatsgruppe != null) {
                    Triple(brukerprofil, innsatsgruppe, message.value.fattetDato)
                } else {
                    null
                }
            }.forEach { (brukerprofil, innsatsgruppe, fattetDato) ->

            }
    }
}