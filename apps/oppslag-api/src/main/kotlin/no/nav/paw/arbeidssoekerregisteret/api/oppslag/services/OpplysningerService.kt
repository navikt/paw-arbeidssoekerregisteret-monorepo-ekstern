package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toOpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerRepository
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import java.util.*

class OpplysningerService(private val opplysningerRepository: OpplysningerRepository) {

    fun finnOpplysningerForPeriodeId(periodeId: UUID): List<OpplysningerOmArbeidssoekerResponse> =
        opplysningerRepository.finnOpplysningerForPeriodeId(periodeId)
            .map { it.toOpplysningerOmArbeidssoekerResponse() }

    fun finnOpplysningerForIdentiteter(identitetsnummerList: List<Identitetsnummer>): List<OpplysningerOmArbeidssoekerResponse> =
        opplysningerRepository.finnOpplysningerForIdentiteter(identitetsnummerList)
            .map { it.toOpplysningerOmArbeidssoekerResponse() }

    fun lagreAlleOpplysninger(opplysninger: Sequence<OpplysningerOmArbeidssoeker>) =
        opplysningerRepository.lagreAlleOpplysninger(opplysninger)
}
