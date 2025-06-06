package no.nav.paw.test.data.periode

import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import java.util.UUID

fun createOpplysninger(
    id: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    helse: Helse? = createHelse(),
    annet: Annet? = createAnnet(),
    sendtInnAv: Metadata = MetadataFactory.create().build(),
    jobbsituasjon: Jobbsituasjon = createJobbsituasjon(),
    utdanning: Utdanning? = createUtdanning()
): OpplysningerOmArbeidssoeker {
    return OpplysningerOmArbeidssoeker.newBuilder()
        .setPeriodeId(periodeId)
        .setId(id)
        .setHelse(helse)
        .setAnnet(annet)
        .setSendtInnAv(sendtInnAv)
        .setJobbsituasjon(jobbsituasjon)
        .setUtdanning(utdanning)
        .build()
}

fun createHelse(
    harHelseutfordringer: Boolean? = false
): Helse {
    return Helse(
        booleanTilJaNeiVetIkke(harHelseutfordringer)
    )
}

fun createAnnet(
    harAnnet: Boolean? = false
): Annet {
    return Annet(
        booleanTilJaNeiVetIkke(harAnnet)
    )
}

private fun booleanTilJaNeiVetIkke(harAnnet: Boolean?): JaNeiVetIkke = when (harAnnet) {
    true -> JaNeiVetIkke.JA
    false -> JaNeiVetIkke.NEI
    null -> JaNeiVetIkke.VET_IKKE
}

fun createJobbsituasjon(
    beskrivelseMedDetaljer: List<Pair<Beskrivelse, Map<String, String>>> = listOf(
        Beskrivelse.HAR_BLITT_SAGT_OPP to mapOf("dato" to "2023-01-01", "årsak" to "Omorganisering"),
        Beskrivelse.DELTIDSJOBB_VIL_MER to mapOf("arbeidstid" to "20 timer per uke", "ønsket_stilling" to "fulltid")
    )
): Jobbsituasjon {
    return Jobbsituasjon(beskrivelseMedDetaljer.map {
        BeskrivelseMedDetaljer(it.first, it.second)
    })
}

fun createUtdanning(
    nus: String = "1234",
    godkjent: Boolean? = true,
    bestatt: Boolean? = true
): Utdanning {
    return Utdanning(
        nus,
        booleanTilJaNeiVetIkke(bestatt),
        booleanTilJaNeiVetIkke(godkjent)
    )
}