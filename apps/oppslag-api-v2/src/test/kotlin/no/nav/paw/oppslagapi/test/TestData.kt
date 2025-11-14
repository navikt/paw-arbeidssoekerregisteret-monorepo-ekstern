package no.nav.paw.oppslagapi.test

import io.opentelemetry.api.trace.Span
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.oppslagapi.data.consumer.toRow
import no.nav.paw.oppslagapi.data.serde
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import no.nav.paw.test.data.bekreftelse.startPaaVegneAv
import no.nav.paw.test.data.bekreftelse.stoppPaaVegneAv
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
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

    val person1 = listOf(dnr1, fnr1)
    val person2 = listOf(dnr2, fnr2)

    val personer = listOf(person1, person2)

    val navAnstatt1 = navAnsatt(
        navId = navId1
    )
    val navAnstatt2 = navAnsatt(
        navId = navId2
    )
    val navAnstatt3 = navAnsatt(
        navId = navId3
    )

    val periode_a_startet = periode(identitetsnummer = Identitetsnummer("12345678901"), startet = Instant.now())
    val periode_a_opplysninger = createOpplysninger(
        periodeId = periode_a_startet.id,
        sendtInnAv = metadata(tidspunkt = Instant.now())
    )
    val periode_a_paa_vegne_av_startet = startPaaVegneAv(periodeId = periode_a_startet.id)
    val periode_a_paa_vegne_av_stoppet = stoppPaaVegneAv(periodeId = periode_a_startet.id)
    val periode_a_bekreftelse = bekreftelseMelding(periodeId = periode_a_startet.id)
    val periode_a_avsluttet = periode(
        periodeId = periode_a_startet.id,
        identitetsnummer = Identitetsnummer(periode_a_startet.identitetsnummer),
        avsluttet = Instant.now() + Duration.ofMinutes(1),
        startet = periode_a_startet.startet.tidspunkt
    )
    val periode_b_startet = periode(identitetsnummer = Identitetsnummer("12345678902"), startet = Instant.now())
    val periode_b_opplysninger = createOpplysninger(
        periodeId = periode_b_startet.id,
        sendtInnAv = metadata(tidspunkt = Instant.now())
    )
    val periode_b_avsluttet = periode(
        periodeId = periode_b_startet.id,
        identitetsnummer = Identitetsnummer(periode_b_startet.identitetsnummer),
        avsluttet = Instant.now() + Duration.ofMinutes(1),
        startet = periode_b_startet.startet.tidspunkt
    )
    val periode_c_startet = periode(identitetsnummer = Identitetsnummer("12345678902"), startet = Instant.now())
    val periode_c_opplysninger = createOpplysninger(
        periodeId = periode_c_startet.id,
        sendtInnAv = metadata(tidspunkt = Instant.now())
    )
    val periode_c_profilering = createProfilering(
        periodeId = periode_c_startet.id,
        opplysningerId = periode_c_opplysninger.id,
        sendtInnAv = metadata(tidspunkt = Instant.now())
    )
    val periode_c_avsluttet = periode(
        periodeId = periode_c_startet.id,
        identitetsnummer = Identitetsnummer(periode_c_startet.identitetsnummer),
        avsluttet = Instant.now() + Duration.ofMinutes(1),
        startet = periode_c_startet.startet.tidspunkt
    )

    val data = listOf(
        periode_a_startet,
        periode_a_opplysninger,
        periode_a_paa_vegne_av_startet,
        periode_a_paa_vegne_av_stoppet,
        periode_a_bekreftelse,
        periode_a_avsluttet,
        periode_b_startet,
        periode_b_opplysninger,
        periode_b_avsluttet,
        periode_c_startet,
        periode_c_opplysninger,
        periode_c_profilering,
        periode_c_avsluttet
    )

    val dataRows
        get() =
            data
                .map(SpecificRecord::asConsumerRecord)
                .map { it.toRow(serde.deserializer()) to Span.current() }

    val tilgangsConfig = tilgang(navId1, person1, Tilgang.LESE, true) +
            tilgang(navId1, person2, Tilgang.LESE, true) +
            tilgang(navId2, person1, Tilgang.LESE, true) +
            tilgang(navId2, person2, Tilgang.LESE, true) +
            tilgang(navId3, person1, Tilgang.LESE, false) +
            tilgang(navId3, person2, Tilgang.LESE, true)

    fun periode(
        periodeId: UUID = UUID.randomUUID(),
        identitetsnummer: Identitetsnummer,
        startet: Instant,
        avsluttet: Instant? = null
    ) = PeriodeFactory.create()
        .build(
            id = periodeId,
            identitetsnummer = identitetsnummer.value,
            startet = MetadataFactory.create().build(tidspunkt = startet),
            avsluttet = avsluttet?.let { MetadataFactory.create().build(tidspunkt = it) }
        )

    fun metadata(
        tidspunkt: Instant
    ) = MetadataFactory.create()
        .build(
            tidspunkt = tidspunkt
        )

    fun navAnsatt(
        navId: NavIdent
    ): NavAnsatt {
        return NavAnsatt(
            oid = UUID.randomUUID(),
            ident = navId.value,
            sikkerhetsnivaa = "azure:Level4"
        )
    }

    fun tilgang(
        ansatt: NavIdent,
        person: List<Identitetsnummer>,
        tilgangsType: Tilgang,
        harTilgang: Boolean
    ): List<TilgangsConfig> {
        return person.map { identitetsnummer ->
            TilgangsConfig(
                ident = ansatt,
                identitetsnummer = identitetsnummer,
                tilgangsType = tilgangsType,
                harTilgang = harTilgang
            )
        }
    }
}

data class TilgangsConfig(
    val ident: NavIdent,
    val identitetsnummer: Identitetsnummer,
    val tilgangsType: Tilgang,
    val harTilgang: Boolean
)