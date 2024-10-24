package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import java.util.*

class ProfileringService(private val profileringRepository: ProfileringRepository) {

    fun finnProfileringerForPeriodeId(
        periodeId: UUID,
        paging: Paging = Paging()
    ): List<ProfileringResponse> =
        profileringRepository.finnProfileringerForPeriodeId(periodeId, paging)
            .map { it.toProfileringResponse() }

    fun finnProfileringerForIdentiteter(
        identitetsnummerList: List<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<ProfileringResponse> =
        profileringRepository.finnProfileringerForIdentiteter(identitetsnummerList, paging)
            .map { it.toProfileringResponse() }

    fun lagreAlleProfileringer(profileringer: Sequence<Profilering>) =
        profileringRepository.lagreAlleProfileringer(profileringer)
}
