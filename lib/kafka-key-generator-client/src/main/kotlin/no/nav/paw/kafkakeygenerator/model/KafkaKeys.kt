package no.nav.paw.kafkakeygenerator.model

data class KafkaKeysRequest(
    val ident: String,
)

data class KafkaKeysResponse(
    val id: Long,
    val key: Long,
)

data class KafkaKeysInfoResponse(
    val info: Info,
)

data class Info(
    val identitetsnummer: String,
    val lagretData: LokalIdData?,
    val pdlData: PdlData
)

data class LokalIdData(
    val arbeidsoekerId: Long,
    val recordKey: Long
)

data class PdlData(
    val error: String?,
    val id: List<PdlId>?
)

data class PdlId(
    val gruppe: String,
    val id: String,
    val gjeldende: Boolean
)