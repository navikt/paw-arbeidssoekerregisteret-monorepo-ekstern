package no.naw.paw.ledigestillinger.hwm

data class Hwm(
    val topic: String,
    val partition: Int,
    val offset: Long
)