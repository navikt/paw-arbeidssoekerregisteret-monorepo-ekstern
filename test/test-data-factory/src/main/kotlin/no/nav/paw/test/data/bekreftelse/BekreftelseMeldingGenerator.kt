package no.nav.paw.test.data.bekreftelse

import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import java.time.Instant
import java.util.*

fun bekreftelseMelding(
    id: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
    gjelderFra: Instant = Instant.now(),
    gjelderTil: Instant = Instant.now(),
    harJobbetIDennePerioden: Boolean = true,
    vilFortsetteSomArbeidssoeker: Boolean = true,
    tidspunkt: Instant = Instant.now(),
) =
    Bekreftelse
        .newBuilder()
        .setPeriodeId(periodeId)
        .setBekreftelsesloesning(bekreftelsesloesning)
        .setId(id)
        .setSvar(
            Svar
            .newBuilder()
            .setSendtInnAv(
                Metadata
                    .newBuilder()
                    .setTidspunkt(tidspunkt)
                    .setUtfoertAv(
                        Bruker
                            .newBuilder()
                            .setId("test")
                            .setType(BrukerType.SLUTTBRUKER)
                            .build()
                    ).setKilde("test")
                    .setAarsak("test")
                    .build()
            )
            .setGjelderFra(gjelderFra)
            .setGjelderTil(gjelderTil)
            .setHarJobbetIDennePerioden(harJobbetIDennePerioden)
            .setVilFortsetteSomArbeidssoeker(vilFortsetteSomArbeidssoeker)
            .build())
        .build()