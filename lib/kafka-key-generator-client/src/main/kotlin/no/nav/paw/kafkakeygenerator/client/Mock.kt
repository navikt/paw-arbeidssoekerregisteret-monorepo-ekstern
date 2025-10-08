package no.nav.paw.kafkakeygenerator.client

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

fun inMemoryKafkaKeysMock(): KafkaKeysClient {
    val naisClusterName = System.getenv("NAIS_CLUSTER_NAME")
    if (naisClusterName != null) {
        throw IllegalStateException("Kan ikke bruke inMemoryKafkaKeysMock i $naisClusterName")
    }
    val sekvens = AtomicLong(0)
    val map: ConcurrentMap<String, Long> = ConcurrentHashMap()
    return object : KafkaKeysClient {
        override suspend fun getIdAndKeyOrNull(identitetsnummer: String): KafkaKeysResponse {
            val id = map.computeIfAbsent(identitetsnummer) { sekvens.incrementAndGet() }
            return KafkaKeysResponse(id, id % 2)
        }

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

        override suspend fun getIdentiteter(
            identitetsnummer: String,
            visKonflikter: Boolean,
            hentPdl: Boolean
        ) = IdentiteterResponse(
            arbeidssoekerId = getIdAndKeyOrNull(identitetsnummer).id,
            recordKey = getIdAndKeyOrNull(identitetsnummer).key,
            identiteter = listOf(
                Identitet(
                    identitet = identitetsnummer,
                    type = IdentitetType.FOLKEREGISTERIDENT,
                    gjeldende = true
                )
            ),
            pdlIdentiteter = if (hentPdl) {
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
}
