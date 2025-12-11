package no.naw.paw.minestillinger.vedtak14a

import io.opentelemetry.api.common.AttributeKey.booleanKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.felles.model.AktorId
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.hwm.Message
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlagg
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
                brukerprofil?.let { it to message }
            }
            .filter { (brukerprofil, message) ->
                val harAktivPeriode = brukerprofil.arbeidssoekerperiodeAvsluttet == null
                val fattet = message.value.fattetDato?.toInstant()
                val profileringsTidspunkt = brukerprofil.flagg<HarGodeMuligheterFlagg>()?.tidspunkt ?: Instant.EPOCH
                val fattetEtterProfilering = fattet != null && fattet.isAfter(profileringsTidspunkt)
                val harInnsatsgruppe = message.value.innsatsgruppe != null
                Span.current().addEvent(
                    "vedtaksfilter", Attributes.of(
                        booleanKey("vedtak_er_etter_profilering"), fattetEtterProfilering,
                        booleanKey("har_aktiv_periode"), harAktivPeriode,
                        booleanKey("har_innsatsgruppe"), harInnsatsgruppe
                    )
                )
                harAktivPeriode && fattetEtterProfilering && harInnsatsgruppe
            }
            .forEach { (profil, melding) ->
                val brukerId = profil.id
                val vedtattStdInnsats = melding.value.innsatsgruppe == Innsatsgruppe.STANDARD_INNSATS
                brukerprofilTjeneste.skrivFlagg(
                    brukerId,
                    listOf(StandardInnsatsFlaggtype.flagg(
                        verdi = vedtattStdInnsats,
                        tidspunkt = melding.value.fattetDato!!.toInstant() //Filter på fattetEtterProfilering sikrer non-null her
                    ))
                )
            }
    }
}