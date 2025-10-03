package no.nav.paw.hwm

interface OnAssigned {
    fun assigned(topic: String, partition: Int)
}

object NoAdditionalActionsOnAssigned : OnAssigned {
    override fun assigned(topic: String, partition: Int) {}
}