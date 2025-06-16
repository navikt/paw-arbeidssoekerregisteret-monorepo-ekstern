package no.nav.paw.oppslagapi.dataconsumer.kafka.hwm

data class Hwm(
    val topic: String,
    val partition: Int,
    val offset: Long
)