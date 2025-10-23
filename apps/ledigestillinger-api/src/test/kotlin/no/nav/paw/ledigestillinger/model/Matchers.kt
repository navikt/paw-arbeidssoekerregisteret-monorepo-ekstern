package no.nav.paw.ledigestillinger.model

import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.util.toLocalDateTimeString
import java.time.ZoneOffset

infix fun StillingRow.shouldBeEqualTo(other: Ad) {
    uuid.toString() shouldBe other.uuid
    adnr shouldBe other.adnr
    tittel shouldBe other.title
    status shouldBe other.status.asStillingStatus()
    visning shouldBe other.privacy.asVisningGrad()
    kilde shouldBe other.source
    medium shouldBe other.medium
    referanse shouldBe other.reference
    arbeidsgivernavn shouldBe other.businessName
    stillingstittel shouldBe other.properties?.find { it.key == "jobtitle" }?.value
    ansettelsesform shouldBe other.properties?.find { it.key == "engagementtype" }?.value
    stillingsprosent shouldBe other.properties?.find { it.key == "extent" }?.value
    stillingsantall shouldBe other.properties?.find { it.key == "positioncount" }?.value
    sektor shouldBe other.properties?.find { it.key == "sector" }?.value
    soeknadsfrist shouldBe other.properties?.find { it.key == "applicationdue" }?.value
    oppstartsfrist shouldBe other.properties?.find { it.key == "starttime" }?.value
    opprettetTimestamp.atZone(ZoneOffset.UTC).toLocalDateTime()
        .toLocalDateTimeString() shouldBe other.created
    endretTimestamp.atZone(ZoneOffset.UTC).toLocalDateTime()
        .toLocalDateTimeString() shouldBe other.updated
    publisertTimestamp.atZone(ZoneOffset.UTC).toLocalDateTime()
        .toLocalDateTimeString() shouldBe other.published
    arbeidsgiver?.asDto() shouldBe other.employer?.asDto()
    kategorier.map { it.asDto() } shouldContainOnly other.categories.map { it.asDto() }
    klassifiseringer.map { it.asDto() } shouldContainOnly other.classifications.map { it.asDto() }
    lokasjoner.map { it.asDto() } shouldContainOnly other.locations.map { it.asDto() }
    egenskaper.map { it.asDto() } shouldContainOnly other.properties.map { it.asDto() }
}