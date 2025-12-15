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
    vedtak: Siste14aVedtakMelding
) {
    vedtak.aktorId?.let { aktørId ->
        val brukerprofil = AktorId(aktørId)
            .let(idFunction)
            ?.let(brukerprofilTjeneste::hentLokalBrukerProfilEllerNull)
        brukerprofil?.let { it to vedtak }
    }.also { res ->
        println("Vi er her")
        Span.current().addEvent(
            "brukerprofil_mapping", Attributes.of(
                booleanKey("brukerprofil_funnet"), res != null,
                booleanKey("vedtak_har_aktorId"), vedtak.aktorId != null
            )
        )
    }
        ?.takeIf { (brukerprofil, vedtak) ->
            val harAktivPeriode = brukerprofil.arbeidssoekerperiodeAvsluttet == null
            val fattet = vedtak.fattetDato?.toInstant()
            val profileringsTidspunkt = brukerprofil.flagg<HarGodeMuligheterFlagg>()?.tidspunkt ?: Instant.EPOCH
            val fattetEtterProfilering = fattet != null && fattet.isAfter(profileringsTidspunkt)
            val harInnsatsgruppe = vedtak.innsatsgruppe != null
            Span.current().addEvent(
                "vedtaksfilter", Attributes.of(
                    booleanKey("vedtak_er_etter_profilering"), fattetEtterProfilering,
                    booleanKey("har_aktiv_periode"), harAktivPeriode,
                    booleanKey("har_innsatsgruppe"), harInnsatsgruppe
                )
            )
            harAktivPeriode && fattetEtterProfilering && harInnsatsgruppe
        }
        ?.run {
            val (profil, vedtak) = this
            val brukerId = profil.id
            val vedtattStdInnsats = vedtak.innsatsgruppe == Innsatsgruppe.STANDARD_INNSATS
            brukerprofilTjeneste.skrivFlagg(
                brukerId,
                listOf(
                    StandardInnsatsFlaggtype.flagg(
                        verdi = vedtattStdInnsats,
                        tidspunkt = vedtak.fattetDato!!.toInstant() //Filter på fattetEtterProfilering sikrer non-null her
                    )
                )
            )
        }
}