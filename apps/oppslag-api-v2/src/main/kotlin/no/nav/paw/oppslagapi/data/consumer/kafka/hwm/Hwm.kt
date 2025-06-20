package no.nav.paw.oppslagapi.data.consumer.kafka.hwm

data class Hwm(
    val topic: String,
    val partition: Int,
    val offset: Long
)