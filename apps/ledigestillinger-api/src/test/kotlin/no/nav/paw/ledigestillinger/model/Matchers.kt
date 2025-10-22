package no.nav.paw.ledigestillinger.model

import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.Classification
import no.nav.pam.stilling.ext.avro.Company
import no.nav.pam.stilling.ext.avro.Location
import no.nav.pam.stilling.ext.avro.Property
import no.nav.pam.stilling.ext.avro.StyrkCategory
import no.nav.paw.ledigestillinger.api.models.Arbeidsgiver
import no.nav.paw.ledigestillinger.api.models.Egenskap
import no.nav.paw.ledigestillinger.api.models.Kategori
import no.nav.paw.ledigestillinger.api.models.Klassifisering
import no.nav.paw.ledigestillinger.api.models.Lokasjon
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

fun Company.asDto(): Arbeidsgiver = Arbeidsgiver(
    orgForm = this.orgform,
    orgNr = this.orgnr,
    parentOrgNr = this.parentOrgnr,
    navn = this.name,
    offentligNavn = this.publicName
)

fun StyrkCategory.asDto(): Kategori = Kategori(
    kode = this.styrkCode,
    normalisertKode = this.styrkCode.asNormalisertKode(),
    navn = this.name
)

fun Classification.asDto(): Klassifisering = Klassifisering(
    type = this.categoryType,
    kode = this.code,
    navn = this.name
)

fun Location.asDto(): Lokasjon = Lokasjon(
    poststed = this.city,
    postkode = this.postalCode,
    kommune = this.municipal,
    kommunenummer = this.municipalCode,
    fylke = this.county,
    fylkesnummer = this.countyCode,
    land = this.country
)

fun Property.asDto(): Egenskap = Egenskap(
    key = this.key,
    value = this.value
)