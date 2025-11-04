package no.nav.paw.kafkakeygenerator.client

import no.nav.paw.kafkakeygenerator.model.Identitet
import no.nav.paw.kafkakeygenerator.model.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentiteterResponse
import no.nav.paw.kafkakeygenerator.model.Info
import no.nav.paw.kafkakeygenerator.model.KafkaKeysInfoResponse
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import no.nav.paw.kafkakeygenerator.model.LokalIdData
import no.nav.paw.kafkakeygenerator.model.PdlData
import no.nav.paw.kafkakeygenerator.model.PdlId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

class MockKafkaKeysClient(
    private val sekvens: AtomicLong = AtomicLong(0),
    private val map: ConcurrentMap<String, Long> = ConcurrentHashMap()
) : KafkaKeysClient {

    override suspend fun getIdAndKeyOrNull(identitetsnummer: String): KafkaKeysResponse {
        val id = map.computeIfAbsent(identitetsnummer) { sekvens.incrementAndGet() }
        return KafkaKeysResponse(id, id % 2)
    }

    override suspend fun getIdentiteter(
        identitetsnummer: String,
        visKonflikter: Boolean,
        hentFraPdl: Boolean
    ): IdentiteterResponse {
        val idAndKey = getIdAndKeyOrNull(identitetsnummer)
        return IdentiteterResponse(
            arbeidssoekerId = idAndKey.id,
            recordKey = idAndKey.key,
            identiteter = listOf(
                Identitet(
                    identitet = identitetsnummer,
                    type = IdentitetType.FOLKEREGISTERIDENT,
                    gjeldende = true
                )
            ),
            pdlIdentiteter = if (hentFraPdl) {
                listOf(
                    Identitet(
                        identitet = identitetsnummer,
                        type = IdentitetType.FOLKEREGISTERIDENT,
                        gjeldende = true
                    )
                )
            } else {
                null
            },
            konflikter = emptyList()
        )
    }

    @Deprecated("Denne vil bli fjernet", ReplaceWith("getIdentiteter(identitetsnummer)"))
    override suspend fun getInfo(identitetsnummer: String): KafkaKeysInfoResponse? {
        val idAndKey = getIdAndKeyOrNull(identitetsnummer)
        return KafkaKeysInfoResponse(
            info = Info(
                identitetsnummer = identitetsnummer,
                lagretData = LokalIdData(
                    arbeidsoekerId = idAndKey.id,
                    recordKey = idAndKey.key,
                ),
                pdlData = PdlData(
                    error = null,
                    id = listOf(PdlId(gruppe = "FOLKEREGISTERIDENT", id = identitetsnummer, gjeldende = true))
                )
            )
        )
    }
}
