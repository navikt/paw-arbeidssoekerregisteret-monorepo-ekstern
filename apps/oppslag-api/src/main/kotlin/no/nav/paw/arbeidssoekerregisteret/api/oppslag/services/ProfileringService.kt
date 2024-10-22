package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import java.util.*

class ProfileringService(private val profileringRepository: ProfileringRepository) {

    fun finnProfileringerForPeriodeId(periodeId: UUID): List<ProfileringResponse> =
        profileringRepository.finnProfileringerForPeriodeId(periodeId)
            .map { it.toProfileringResponse() }

    fun finnProfileringerForIdentiteter(identitetsnummerList: List<Identitetsnummer>): List<ProfileringResponse> =
        profileringRepository.finnProfileringerForIdentiteter(identitetsnummerList)
            .map { it.toProfileringResponse() }

    fun lagreAlleProfileringer(batch: Sequence<Profilering>) = profileringRepository.lagreAlleProfileringer(batch)
}
