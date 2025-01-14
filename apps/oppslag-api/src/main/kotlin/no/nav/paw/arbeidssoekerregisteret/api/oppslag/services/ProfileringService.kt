package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.*

class ProfileringService(private val profileringRepository: ProfileringRepository) {

    fun finnProfileringerForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<ProfileringResponse> =
        profileringRepository.finnProfileringerForPeriodeIdList(periodeIdList, paging)
            .map { it.toProfileringResponse() }

    fun finnProfileringerForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<ProfileringResponse> =
        profileringRepository.finnProfileringerForIdentiteter(identitetsnummerList, paging)
            .map { it.toProfileringResponse() }

    fun lagreAlleProfileringer(profileringer: Iterable<Profilering>) =
        profileringRepository.lagreAlleProfileringer(profileringer)

    fun handleRecords(records: ConsumerRecords<Long, Profilering>) =
        lagreAlleProfileringer(records.map { it.value() })
}
