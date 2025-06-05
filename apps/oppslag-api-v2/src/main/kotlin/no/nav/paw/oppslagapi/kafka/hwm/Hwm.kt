package no.nav.paw.oppslagapi.kafka.hwm

data class Hwm(
    val topic: String,
    val partition: Int,
    val offset: Long
)