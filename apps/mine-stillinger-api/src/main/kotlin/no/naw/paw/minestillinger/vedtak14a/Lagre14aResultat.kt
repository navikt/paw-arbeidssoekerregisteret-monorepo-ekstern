package no.naw.paw.minestillinger.vedtak14a

import no.nav.paw.felles.model.AktorId
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.hwm.Message
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.brukerprofil.flagg.StandardInnsatsFlaggtype
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

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
                val fattetTidspunkt = message.value.fattetDato?.toInstant() ?: Instant.EPOCH
                val brukerId = when {
                    brukerprofil == null -> null
                    brukerprofil.arbeidssoekerperiodeAvsluttet != null -> null
                    else -> brukerprofil.id
                }
                brukerId?.let {
                    it to StandardInnsatsFlaggtype.flagg(
                        verdi = innsatsgruppe == Innsatsgruppe.STANDARD_INNSATS,
                        tidspunkt = fattetTidspunkt
                    )
                }
            }.forEach { (brukerId, flagg) ->
                brukerprofilTjeneste.skrivFlagg(brukerId, listOf(flagg))
            }
    }
}