package no.nav.paw.ledigestillinger.model.dao

import no.naw.paw.ledigestillinger.model.StillingStatus

data class StillingStatusCountRow(
    val status: StillingStatus,
    val count: Long
)
