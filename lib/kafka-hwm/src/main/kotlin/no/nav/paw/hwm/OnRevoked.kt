package no.nav.paw.hwm

interface OnRevoked {
    fun revoked(topic: String, partition: Int)
}

object NoAdditionalActionsOnRevoked : OnRevoked {
    override fun revoked(topic: String, partition: Int) {}
}