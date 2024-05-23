package no.nav.paw.rapportering.api.kafka

import kotlinx.coroutines.runBlocking
import no.nav.paw.rapportering.api.createKafkaKeyGeneratorClient

fun createIdAndRecordKeyFunction(): KafkaIdAndRecordKeyFunction =
    with(createKafkaKeyGeneratorClient()) {
        KafkaIdAndRecordKeyFunction { identitetsnummer ->
            runBlocking {
                getIdAndKey(identitetsnummer)
                    ?.let {
                        IdAndRecordKey(
                            id = it.id,
                            recordKey = it.key
                        )
                    }
            }
        }
    }

fun interface KafkaIdAndRecordKeyFunction {
    operator fun invoke(identitetsnummer: String): IdAndRecordKey?
}

data class IdAndRecordKey(
    val id: Long,
    val recordKey: Long
)