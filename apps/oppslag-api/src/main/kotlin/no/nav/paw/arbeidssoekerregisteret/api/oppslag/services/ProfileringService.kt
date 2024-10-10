package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import java.util.*

class ProfileringService(private val profileringRepository: ProfileringRepository) {

    fun finnProfileringerForPeriodeId(periodeId: UUID) = profileringRepository.finnProfileringerForPeriodeId(periodeId)

    fun finnProfileringerForIdentiteter(identitetsnummerList: List<Identitetsnummer>) =
        profileringRepository.finnProfileringerForIdentiteter(identitetsnummerList)

    fun lagreAlleProfileringer(batch: Sequence<Profilering>) = profileringRepository.lagreAlleProfileringer(batch)
}
