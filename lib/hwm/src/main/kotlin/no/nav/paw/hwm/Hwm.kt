package no.nav.paw.hwm

data class Hwm(
    val topic: String,
    val partition: Int,
    val offset: Long
)

