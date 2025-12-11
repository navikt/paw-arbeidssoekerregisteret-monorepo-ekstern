package no.nav.paw.oppslagapi.test

import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.consumer.converters.toOpenApi
import no.nav.paw.oppslagapi.data.consumer.toRow
import no.nav.paw.oppslagapi.data.egenvurdering_v1
import no.nav.paw.oppslagapi.data.opplysninger_om_arbeidssoeker_v4
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.data.profilering_v1
import no.nav.paw.oppslagapi.data.serde
import no.nav.paw.security.authentication.model.Anonym
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import no.nav.paw.test.data.bekreftelse.startPaaVegneAv
import no.nav.paw.test.data.bekreftelse.stoppPaaVegneAv
import no.nav.paw.test.data.periode.BrukerFactory
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.paw.test.data.periode.createEgenvurdering
import no.nav.paw.test.data.periode.createOpplysninger
import no.nav.paw.test.data.periode.createProfilering
import no.nav.paw.tilgangskontroll.client.Tilgang
import org.apache.avro.specific.SpecificRecord
import java.time.Duration
import java.time.Instant
import java.util.*

object TestData {

    val navId1 = NavIdent("Z20001")
    val navId2 = NavIdent("Z20002")
    val navId3 = NavIdent("Z20003")
    val dnr1 = Identitetsnummer("41017012345")
    val fnr1 = Identitetsnummer("01017012345")
    val dnr2 = Identitetsnummer("42017012345")
    val fnr2 = Identitetsnummer("02017012345")
    val dnr3 = Identitetsnummer("43017012345")
    val fnr3 = Identitetsnummer("03017012345")
    val dnr4 = Identitetsnummer("44017012345")
    val fnr4 = Identitetsnummer("04017012345")
    val dnr5 = Identitetsnummer("45017012345")
    val fnr5 = Identitetsnummer("05017012345")

    val bruker1 = sluttbruker(listOf(dnr1, fnr1))
    val bruker2 = sluttbruker(listOf(dnr2, fnr2))
    val bruker3 = sluttbruker(listOf(dnr3, fnr3))
    val bruker4 = sluttbruker(listOf(dnr4, fnr4))
    val bruker5 = sluttbruker(listOf(dnr5, fnr5))

    val brukere = listOf(bruker1, bruker2, bruker3, bruker4, bruker5)

    val anstatt1 = navAnsatt(
        navId = navId1
    )
    val anstatt2 = navAnsatt(
        navId = navId2
    )
    val anstatt3 = navAnsatt(
        navId = navId3
    )

    val periode1_1_startet = periode(
        identitetsnummer = fnr1,
        startet = Instant.now()
    )
    val opplysninger1_1 = oppslysninger(
        identitetsnummer = fnr1,
        periodeId = periode1_1_startet.id,
        tidspunkt = Instant.now() + Duration.ofMinutes(1)
    )
    val profilering1_1 = profilering(
        identitetsnummer = fnr1,
        periodeId = periode1_1_startet.id,
        opplysningerId = opplysninger1_1.id,
        tidspunkt = Instant.now() + Duration.ofMinutes(2)
    )
    val egenvurdering1_1 = egenvurdering(
        identitetsnummer = fnr3,
        periodeId = periode1_1_startet.id,
        profileringId = profilering1_1.id,
        tidspunkt = periode1_1_startet.startet.tidspunkt + Duration.ofDays(5)
    )
    val paa_vegne_av1_1_startet = startPaaVegneAv(
        periodeId = periode1_1_startet.id
    )
    val paa_vegne_av1_1_stoppet = stoppPaaVegneAv(
        periodeId = periode1_1_startet.id
    )
    val bekreftelse1_1 = bekreftelse(
        periodeId = periode1_1_startet.id
    )
    val periode1_1_avsluttet = periode(
        identitetsnummer = Identitetsnummer(periode1_1_startet.identitetsnummer),
        periodeId = periode1_1_startet.id,
        avsluttet = Instant.now() + Duration.ofMinutes(1),
        startet = periode1_1_startet.startet.tidspunkt
    )
    val periode2_1_startet = periode(
        identitetsnummer = fnr2,
        startet = Instant.now()
    )
    val opplysninger2_1 = oppslysninger(
        identitetsnummer = fnr2,
        periodeId = periode2_1_startet.id,
        tidspunkt = Instant.now()
    )
    val periode2_1_avsluttet = periode(
        identitetsnummer = Identitetsnummer(periode2_1_startet.identitetsnummer),
        periodeId = periode2_1_startet.id,
        avsluttet = Instant.now() + Duration.ofMinutes(1),
        startet = periode2_1_startet.startet.tidspunkt
    )
    val periode2_2_startet = periode(
        identitetsnummer = fnr2,
        startet = Instant.now()
    )
    val opplysninger2_2 = oppslysninger(
        identitetsnummer = fnr2,
        periodeId = periode2_2_startet.id,
        tidspunkt = Instant.now()
    )
    val profilering2_2 = profilering(
        identitetsnummer = fnr2,
        periodeId = periode2_2_startet.id,
        opplysningerId = opplysninger2_2.id,
        tidspunkt = Instant.now()
    )
    val periode2_2_avsluttet = periode(
        identitetsnummer = Identitetsnummer(periode2_2_startet.identitetsnummer),
        periodeId = periode2_2_startet.id,
        avsluttet = Instant.now() + Duration.ofMinutes(1),
        startet = periode2_2_startet.startet.tidspunkt
    )

    val periode3_1_startet = periode(
        identitetsnummer = fnr3,
        startet = Instant.now() - Duration.ofDays(42)
    )
    val opplysninger3_1 = oppslysninger(
        identitetsnummer = fnr3,
        periodeId = periode3_1_startet.id,
        tidspunkt = periode3_1_startet.startet.tidspunkt + Duration.ofMinutes(1)
    )
    val profilering3_1 = profilering(
        identitetsnummer = fnr3,
        periodeId = periode3_1_startet.id,
        opplysningerId = opplysninger3_1.id,
        tidspunkt = periode3_1_startet.startet.tidspunkt + Duration.ofMinutes(2)
    )
    val egenvurdering3_1 = egenvurdering(
        identitetsnummer = fnr3,
        periodeId = periode3_1_startet.id,
        profileringId = profilering3_1.id,
        tidspunkt = periode3_1_startet.startet.tidspunkt + Duration.ofDays(1)
    )
    val bekreftelse3_1 = bekreftelse(
        periodeId = periode3_1_startet.id,
        tidspunkt = Instant.now() - Duration.ofDays(32)
    )

    val periode4_1_startet = periode(
        identitetsnummer = fnr4,
        startet = Instant.now() - Duration.ofDays(42)
    )
    val periode4_1_avsluttet = periode4_1_startet.asAvsluttet(
        avsluttet = Instant.now() - Duration.ofDays(12)
    )
    val opplysninger4_1 = oppslysninger(
        identitetsnummer = fnr4,
        periodeId = periode4_1_startet.id,
        tidspunkt = periode4_1_startet.startet.tidspunkt + Duration.ofMinutes(1)
    )
    val profilering4_1 = profilering(
        identitetsnummer = fnr4,
        periodeId = periode4_1_startet.id,
        opplysningerId = opplysninger3_1.id,
        tidspunkt = periode4_1_startet.startet.tidspunkt + Duration.ofMinutes(2)
    )
    val egenvurdering4_1 = egenvurdering(
        identitetsnummer = fnr4,
        periodeId = periode4_1_startet.id,
        profileringId = profilering4_1.id,
        tidspunkt = periode4_1_startet.startet.tidspunkt + Duration.ofDays(1)
    )
    val bekreftelse4_1 = bekreftelse(
        periodeId = periode4_1_startet.id,
        tidspunkt = Instant.now() - Duration.ofDays(32)
    )

    val hendelser1 = listOf(
        periode1_1_startet,
        opplysninger1_1,
        paa_vegne_av1_1_startet,
        paa_vegne_av1_1_stoppet,
        bekreftelse1_1,
        periode1_1_avsluttet
    )
    val hendelser2 = listOf(
        periode2_1_startet,
        opplysninger2_1,
        periode2_1_avsluttet,
        periode2_2_startet,
        opplysninger2_2,
        profilering2_2,
        periode2_2_avsluttet
    )
    val hendelser3 = listOf(
        periode3_1_startet,
        opplysninger3_1,
        profilering3_1,
        egenvurdering3_1,
        bekreftelse3_1
    )
    val hendelser4 = listOf(
        periode4_1_startet,
        periode4_1_avsluttet,
        opplysninger4_1,
        profilering4_1,
        egenvurdering4_1,
        bekreftelse4_1
    )

    val data1 = hendelser1
        .map(SpecificRecord::asConsumerRecord)
        .map { it.toRow(serde.deserializer()) to Span.current() }
    val data2 = hendelser2
        .map(SpecificRecord::asConsumerRecord)
        .map { it.toRow(serde.deserializer()) to Span.current() }
    val data = data1 + data2

    val rows3: List<Row<Any>> = hendelser3
        .map { it.asRow() }
    val rows4: List<Row<Any>> = hendelser4
        .map { it.asRow() }

    val tilgangsConfig: List<TilgangsConfig> = tilgang(navId1, bruker1, Tilgang.LESE, true) +
            tilgang(navId1, bruker2, Tilgang.LESE, true) +
            tilgang(navId1, bruker3, Tilgang.LESE, true) +
            tilgang(navId1, bruker4, Tilgang.LESE, true) +
            tilgang(navId1, bruker5, Tilgang.LESE, true) +
            tilgang(navId2, bruker1, Tilgang.LESE, true) +
            tilgang(navId2, bruker2, Tilgang.LESE, true) +
            tilgang(navId2, bruker3, Tilgang.LESE, true) +
            tilgang(navId2, bruker4, Tilgang.LESE, true) +
            tilgang(navId2, bruker5, Tilgang.LESE, true) +
            tilgang(navId3, bruker1, Tilgang.LESE, false) +
            tilgang(navId3, bruker2, Tilgang.LESE, true) +
            tilgang(navId3, bruker3, Tilgang.LESE, true) +
            tilgang(navId3, bruker4, Tilgang.LESE, true) +
            tilgang(navId3, bruker5, Tilgang.LESE, true)

    fun periode(
        identitetsnummer: Identitetsnummer = fnr1,
        periodeId: UUID = UUID.randomUUID(),
        startet: Instant = Instant.now(),
        avsluttet: Instant? = null
    ) = PeriodeFactory.create()
        .build(
            id = periodeId,
            identitetsnummer = identitetsnummer.value,
            startet = MetadataFactory.create().build(tidspunkt = startet),
            avsluttet = avsluttet?.let { MetadataFactory.create().build(tidspunkt = it) }
        )

    fun oppslysninger(
        identitetsnummer: Identitetsnummer = fnr1,
        periodeId: UUID = UUID.randomUUID(),
        tidspunkt: Instant = Instant.now()
    ): OpplysningerOmArbeidssoeker = createOpplysninger(
        periodeId = periodeId,
        sendtInnAv = metadata(
            tidspunkt = tidspunkt,
            utfortAv = bruker(
                identitetsnummer = identitetsnummer
            )
        )
    )

    fun profilering(
        identitetsnummer: Identitetsnummer = fnr1,
        periodeId: UUID = UUID.randomUUID(),
        opplysningerId: UUID = UUID.randomUUID(),
        profilertTil: ProfilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER,
        tidspunkt: Instant = Instant.now()
    ): Profilering = createProfilering(
        periodeId = periodeId,
        opplysningerId = opplysningerId,
        profilertTil = profilertTil,
        sendtInnAv = metadata(
            tidspunkt = tidspunkt,
            utfortAv = bruker(
                identitetsnummer = identitetsnummer
            )
        )
    )

    fun egenvurdering(
        identitetsnummer: Identitetsnummer = fnr1,
        periodeId: UUID = UUID.randomUUID(),
        profileringId: UUID = UUID.randomUUID(),
        profilertTil: ProfilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER,
        egenvurdering: ProfilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER,
        tidspunkt: Instant = Instant.now()
    ): Egenvurdering = createEgenvurdering(
        periodeId = periodeId,
        profileringId = profileringId,
        sendtInnAv = metadata(
            tidspunkt = tidspunkt,
            utfortAv = bruker(
                identitetsnummer = identitetsnummer
            )
        ),
        profilertTil = profilertTil,
        egenvurdering = egenvurdering,
    )

    fun bekreftelse(
        periodeId: UUID = UUID.randomUUID(),
        tidspunkt: Instant = Instant.now()
    ): Bekreftelse = bekreftelseMelding(
        periodeId = periodeId,
        tidspunkt = tidspunkt
    )

    fun metadata(
        tidspunkt: Instant = Instant.now(),
        utfortAv: Bruker = bruker()
    ) = MetadataFactory.create()
        .build(
            tidspunkt = tidspunkt,
            utfortAv = utfortAv
        )

    fun bruker(
        identitetsnummer: Identitetsnummer
    ): Bruker = BrukerFactory.create()
        .build(
            brukerType = BrukerType.SLUTTBRUKER,
            id = identitetsnummer.value
        )

    fun bruker(
        navId: NavIdent
    ): Bruker = BrukerFactory.create()
        .build(
            brukerType = BrukerType.VEILEDER,
            id = navId.value
        )

    fun bruker(
        type: BrukerType = BrukerType.SLUTTBRUKER,
        ident: String = fnr1.value
    ): Bruker = BrukerFactory.create()
        .build(
            brukerType = type,
            id = ident
        )

    fun sluttbruker(
        identitetsnummer: List<Identitetsnummer>
    ): Sluttbruker = Sluttbruker(
        ident = identitetsnummer.last(),
        alleIdenter = identitetsnummer.toSet(),
        sikkerhetsnivaa = "tokenx:Level4"
    )

    fun navAnsatt(
        navId: NavIdent
    ): NavAnsatt = NavAnsatt(
        oid = UUID.randomUUID(),
        ident = navId.value,
        sikkerhetsnivaa = "azure:Level4"
    )

    fun anonym(
    ): Anonym = Anonym(
        oid = UUID.randomUUID()
    )

    fun tilgang(
        ansatt: NavIdent,
        person: Sluttbruker,
        tilgangsType: Tilgang,
        harTilgang: Boolean
    ): List<TilgangsConfig> {
        return person.alleIdenter.map { identitetsnummer ->
            TilgangsConfig(
                ident = ansatt,
                identitetsnummer = identitetsnummer,
                tilgangsType = tilgangsType,
                harTilgang = harTilgang
            )
        }
    }
}

fun Periode.asAvsluttet(
    avsluttet: Instant
): Periode = PeriodeFactory.create()
    .build(
        id = id,
        identitetsnummer = identitetsnummer,
        startet = startet,
        avsluttet = MetadataFactory.create()
            .build(tidspunkt = avsluttet)
    )

data class TilgangsConfig(
    val ident: NavIdent,
    val identitetsnummer: Identitetsnummer,
    val tilgangsType: Tilgang,
    val harTilgang: Boolean
)

fun Any.asRow(): Row<Any> {
    when (this) {
        is Periode -> {
            if (this.avsluttet == null) {
                return Row(
                    type = periode_startet_v1,
                    periodeId = this.id,
                    identitetsnummer = this.identitetsnummer,
                    timestamp = this.startet.tidspunkt,
                    data = this.startet.toOpenApi()
                )
            } else {
                return Row(
                    type = periode_avsluttet_v1,
                    periodeId = this.id,
                    identitetsnummer = this.identitetsnummer,
                    timestamp = this.avsluttet.tidspunkt,
                    data = this.startet.toOpenApi()
                )
            }
        }

        is OpplysningerOmArbeidssoeker -> return Row(
            type = opplysninger_om_arbeidssoeker_v4,
            periodeId = this.periodeId,
            identitetsnummer = null,
            timestamp = this.sendtInnAv.tidspunkt,
            data = this.toOpenApi()
        )

        is Bekreftelse -> return Row(
            type = bekreftelsemelding_v1,
            periodeId = this.periodeId,
            identitetsnummer = null,
            timestamp = this.svar.sendtInnAv.tidspunkt,
            data = this.toOpenApi()
        )

        is Profilering -> return Row(
            type = profilering_v1,
            periodeId = this.periodeId,
            identitetsnummer = null,
            timestamp = this.sendtInnAv.tidspunkt,
            data = this.toOpenApi()
        )

        is Egenvurdering -> return Row(
            type = egenvurdering_v1,
            periodeId = this.periodeId,
            identitetsnummer = null,
            timestamp = this.sendtInnAv.tidspunkt,
            data = this.toOpenApi()
        )

        else -> throw IllegalArgumentException("Ukjent type: ${this::class.java.name}")
    }
}