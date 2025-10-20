package no.nav.paw.ledigestillinger.model

import no.nav.paw.ledigestillinger.api.models.Stilling
import no.nav.paw.ledigestillinger.model.dao.StillingRow

fun StillingRow.asDto(): Stilling = Stilling(
    uuid = uuid,
    adnr = adnr,
    tittel = tittel,
    status = status.asDto(),
    arbeidsgiverNavn = arbeidsgiverNavn,
    publisertTimestamp = publisertTimestamp,
    utloeperTimestamp = utloeperTimestamp,
)

fun StillingStatus.asDto(): no.nav.paw.ledigestillinger.api.models.StillingStatus = when (this) {
    StillingStatus.ACTIVE -> no.nav.paw.ledigestillinger.api.models.StillingStatus.ACTIVE
    StillingStatus.INACTIVE -> no.nav.paw.ledigestillinger.api.models.StillingStatus.INACTIVE
    StillingStatus.STOPPED -> no.nav.paw.ledigestillinger.api.models.StillingStatus.STOPPED
    StillingStatus.DELETED -> no.nav.paw.ledigestillinger.api.models.StillingStatus.DELETED
    StillingStatus.REJECTED -> no.nav.paw.ledigestillinger.api.models.StillingStatus.REJECTED
}