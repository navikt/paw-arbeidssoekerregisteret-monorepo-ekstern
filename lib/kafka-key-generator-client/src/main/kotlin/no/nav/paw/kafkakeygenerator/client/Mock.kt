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
            TODO("Implementeres ved behov")
        }
    }
}
