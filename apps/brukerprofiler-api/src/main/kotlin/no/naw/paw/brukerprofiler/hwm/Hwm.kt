package no.naw.paw.brukerprofiler.hwm

data class Hwm(
    val topic: String,
    val partition: Int,
    val offset: Long
)