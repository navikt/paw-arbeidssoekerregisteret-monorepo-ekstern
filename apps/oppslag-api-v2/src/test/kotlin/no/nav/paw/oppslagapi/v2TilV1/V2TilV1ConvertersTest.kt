import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Annet
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.AvviksType
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Helse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Jobbsituasjon
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Profilering
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Svar
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.TidspunktFraKilde
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Utdanning
import no.nav.paw.oppslagapi.v2TilV1.toV1
import no.nav.paw.oppslagapi.v2TilV1.v1Bekreftelse
import no.nav.paw.oppslagapi.v2TilV1.v1Bruker
import no.nav.paw.oppslagapi.v2TilV1.v1Metadata
import no.nav.paw.oppslagapi.v2TilV1.v1Profilering
import no.nav.paw.oppslagapi.v2TilV1.v1TidspunktFraKilde
import java.time.Instant
import java.util.*

//Char-gpt generert test for v2 til v1 konvertering
class V2TilV1ConvertersTest : FreeSpec({
    "OpplysningerOmArbeidssoeker.toV1() should convert and map all fields correctly" {
        val id = UUID.randomUUID()
        val periodeId = UUID.randomUUID()
        val metadata = Metadata(
            tidspunkt = Instant.now(),
            utfoertAv = Bruker(type = Bruker.Type.SYSTEM, id = "brukerId"),
            kilde = "kilde",
            aarsak = "aarsak",
            tidspunktFraKilde = null
        )
        val jobbsituasjonV2 = Jobbsituasjon(
            beskrivelser = listOf(
                BeskrivelseMedDetaljer(
                    beskrivelse = Beskrivelse.ANNET,
                    detaljer = mapOf("key" to "value")
                )
            )
        )
        val utdanningV2 = Utdanning(nus = "nus", bestaatt = JaNeiVetIkke.JA, godkjent = JaNeiVetIkke.NEI)
        val helseV2 = Helse(helsetilstandHindrerArbeid = JaNeiVetIkke.VET_IKKE)
        val annetV2 = Annet(andreForholdHindrerArbeid = JaNeiVetIkke.JA)
        val v2 = OpplysningerOmArbeidssoeker(
            id = id,
            periodeId = periodeId,
            sendtInnAv = metadata,
            jobbsituasjon = jobbsituasjonV2,
            utdanning = utdanningV2,
            helse = helseV2,
            annet = annetV2
        )
        val v1 = v2.toV1()
        v1.opplysningerOmArbeidssoekerId shouldBe id
        v1.periodeId shouldBe periodeId
        v1.sendtInnAv.tidspunkt shouldBe metadata.tidspunkt
        v1.sendtInnAv.kilde shouldBe metadata.kilde
        v1.sendtInnAv.aarsak shouldBe metadata.aarsak
        v1.sendtInnAv.utfoertAv.type.name shouldBe metadata.utfoertAv.type.name
        v1.sendtInnAv.utfoertAv.id shouldBe metadata.utfoertAv.id
        v1.jobbsituasjon.size shouldBe 1
        v1.jobbsituasjon[0].beskrivelse.name shouldBe jobbsituasjonV2.beskrivelser[0].beskrivelse.name
        v1.jobbsituasjon[0].detaljer shouldBe jobbsituasjonV2.beskrivelser[0].detaljer
        v1.utdanning?.nus shouldBe utdanningV2.nus
        v1.utdanning?.bestaatt?.name shouldBe utdanningV2.bestaatt?.name
        v1.utdanning?.godkjent?.name shouldBe utdanningV2.godkjent?.name
        v1.helse?.helsetilstandHindrerArbeid?.name shouldBe helseV2.helsetilstandHindrerArbeid?.name
        v1.annet?.andreForholdHindrerArbeid?.name shouldBe annetV2.andreForholdHindrerArbeid?.name
    }

    "BeskrivelseMedDetaljer.toV1() should convert and map all fields correctly" {
        val v2 = BeskrivelseMedDetaljer(
            beskrivelse = Beskrivelse.ANNET,
            detaljer = mapOf("foo" to "bar")
        )
        val v1 = v2.toV1()
        v1.beskrivelse.name shouldBe v2.beskrivelse.name
        v1.detaljer shouldBe v2.detaljer
    }

    "Utdanning.toV1() should convert and map all fields correctly" {
        val v2 = Utdanning(nus = "nus", bestaatt = JaNeiVetIkke.JA, godkjent = JaNeiVetIkke.NEI)
        val v1 = v2.toV1()
        v1.nus shouldBe v2.nus
        v1.bestaatt?.name shouldBe v2.bestaatt?.name
        v1.godkjent?.name shouldBe v2.godkjent?.name
    }

    "Helse.toV1() should convert and map all fields correctly" {
        val v2 = Helse(helsetilstandHindrerArbeid = JaNeiVetIkke.JA)
        val v1 = v2.toV1()
        v1.helsetilstandHindrerArbeid.name shouldBe v2.helsetilstandHindrerArbeid?.name
    }

    "Annet.toV1() should convert and map all fields correctly" {
        val v2 = Annet(andreForholdHindrerArbeid = JaNeiVetIkke.NEI)
        val v1 = v2.toV1()
        v1.andreForholdHindrerArbeid?.name shouldBe v2.andreForholdHindrerArbeid?.name
    }

    "Bekreftelse.v1Bekreftelse() should convert and map all fields correctly" {
        val periodeId = UUID.randomUUID()
        val metadata = Metadata(
            tidspunkt = Instant.now(),
            utfoertAv = Bruker(type = Bruker.Type.SYSTEM, id = "brukerId"),
            kilde = "kilde",
            aarsak = "aarsak",
            tidspunktFraKilde = null
        )
        val svarV2 = Svar(
            sendtInnAv = metadata,
            gjelderFra = Instant.now(),
            gjelderTil = Instant.now().plusSeconds(3600),
            harJobbetIDennePerioden = true,
            vilFortsetteSomArbeidssoeker = false
        )
        val v2 = Bekreftelse(
            id = UUID.randomUUID(),
            periodeId = periodeId,
            bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
            svar = svarV2
        )
        val v1 = v2.v1Bekreftelse()
        v1.periodeId shouldBe periodeId
        v1.bekreftelsesloesning.name shouldBe v2.bekreftelsesloesning.name
        v1.svar.gjelderFra shouldBe svarV2.gjelderFra
        v1.svar.gjelderTil shouldBe svarV2.gjelderTil
        v1.svar.harJobbetIDennePerioden shouldBe svarV2.harJobbetIDennePerioden
        v1.svar.vilFortsetteSomArbeidssoeker shouldBe svarV2.vilFortsetteSomArbeidssoeker
        v1.svar.sendtInnAv.tidspunkt shouldBe metadata.tidspunkt
        v1.svar.sendtInnAv.kilde shouldBe metadata.kilde
        v1.svar.sendtInnAv.aarsak shouldBe metadata.aarsak
        v1.svar.sendtInnAv.utfoertAv.type.name shouldBe metadata.utfoertAv.type.name
        v1.svar.sendtInnAv.utfoertAv.id shouldBe metadata.utfoertAv.id
    }

    "Profilering.v1Profilering() should convert and map all fields correctly" {
        val id = UUID.randomUUID()
        val periodeId = UUID.randomUUID()
        val opplysningerId = UUID.randomUUID()
        val metadata = Metadata(
            tidspunkt = Instant.now(),
            utfoertAv = Bruker(type = Bruker.Type.SYSTEM, id = "brukerId"),
            kilde = "kilde",
            aarsak = "aarsak",
            tidspunktFraKilde = null
        )
        val v2 = Profilering(
            id = id,
            periodeId = periodeId,
            opplysningerOmArbeidssokerId = opplysningerId,
            sendtInnAv = metadata,
            profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER,
            alder = 30,
            jobbetSammenhengendeSeksAvTolvSisteMnd = true
        )
        val v1 = v2.v1Profilering()
        v1.profileringId shouldBe id
        v1.periodeId shouldBe periodeId
        v1.opplysningerOmArbeidssoekerId shouldBe opplysningerId
        v1.sendtInnAv.tidspunkt shouldBe metadata.tidspunkt
        v1.sendtInnAv.kilde shouldBe metadata.kilde
        v1.sendtInnAv.aarsak shouldBe metadata.aarsak
        v1.sendtInnAv.utfoertAv.type.name shouldBe metadata.utfoertAv.type.name
        v1.sendtInnAv.utfoertAv.id shouldBe metadata.utfoertAv.id
        v1.profilertTil.name shouldBe v2.profilertTil.name
        v1.alder shouldBe v2.alder
        v1.jobbetSammenhengendeSeksAvTolvSisteManeder shouldBe v2.jobbetSammenhengendeSeksAvTolvSisteMnd
    }

    "TidspunktFraKilde.v1TidspunktFraKilde() should convert and map all fields correctly" {
        val tidspunkt = Instant.now()
        val v2 = TidspunktFraKilde(
            tidspunkt = tidspunkt,
            avviksType = AvviksType.FORSINKELSE
        )
        val v1 = v2.v1TidspunktFraKilde()
        v1.tidspunkt shouldBe tidspunkt
        v1.avviksType.name shouldBe v2.avviksType.name
    }

    "Metadata.v1Metadata() should convert and map all fields correctly" {
        val tidspunkt = Instant.now()
        val bruker = Bruker(type = Bruker.Type.SYSTEM, id = "brukerId")
        val tidspunktFraKilde = TidspunktFraKilde(
            tidspunkt = tidspunkt,
            avviksType = AvviksType.RETTING
        )
        val v2 = Metadata(
            tidspunkt = tidspunkt,
            utfoertAv = bruker,
            kilde = "kilde",
            aarsak = "aarsak",
            tidspunktFraKilde = tidspunktFraKilde
        )
        val v1 = v2.v1Metadata()
        v1.tidspunkt shouldBe tidspunkt
        v1.utfoertAv.type.name shouldBe bruker.type.name
        v1.utfoertAv.id shouldBe bruker.id
        v1.kilde shouldBe v2.kilde
        v1.aarsak shouldBe v2.aarsak
        v1.tidspunktFraKilde?.tidspunkt shouldBe tidspunktFraKilde.tidspunkt
        v1.tidspunktFraKilde?.avviksType?.name shouldBe tidspunktFraKilde.avviksType.name
    }

    "Bruker.v1Bruker() should convert and map all fields correctly" {
        val v2 = Bruker(type = Bruker.Type.VEILEDER, id = "brukerId")
        val v1 = v2.v1Bruker()
        v1.type.name shouldBe v2.type.name
        v1.id shouldBe v2.id
    }

})
