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
    }
}
