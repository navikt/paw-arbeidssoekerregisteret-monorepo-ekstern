package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerRepository
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import java.util.*

class OpplysningerService(private val opplysningerRepository: OpplysningerRepository) {

    fun finnOpplysningerForPeriodeId(periodeId: UUID) =
        opplysningerRepository.finnOpplysningerForPeriodeId(periodeId)

    fun finnOpplysningerForIdentiteter(identitetsnummerList: List<Identitetsnummer>) =
        opplysningerRepository.finnOpplysningerForIdentiteter(identitetsnummerList)

    fun lagreAlleOpplysninger(batch: Sequence<OpplysningerOmArbeidssoeker>) =
        opplysningerRepository.lagreAlleOpplysninger(batch)
}
