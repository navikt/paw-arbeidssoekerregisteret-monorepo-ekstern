package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerOmArbeidssoekerRepository
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import java.util.*

class OpplysningerOmArbeidssoekerService(private val opplysningerOmArbeidssoekerRepository: OpplysningerOmArbeidssoekerRepository) {

    fun hentOpplysningerOmArbeidssoeker(periodeId: UUID) = opplysningerOmArbeidssoekerRepository.hentOpplysningerOmArbeidssoeker(periodeId)

    fun hentOpplysningerOmArbeidssoekerMedIdentitetsnummer(identitetsnummer: Identitetsnummer) = opplysningerOmArbeidssoekerRepository.hentOpplysningerOmArbeidssoeker(identitetsnummer)

    fun lagreBatch(batch: Sequence<OpplysningerOmArbeidssoeker>) = opplysningerOmArbeidssoekerRepository.lagreOpplysningerOmArbeidssoeker(batch)
}
