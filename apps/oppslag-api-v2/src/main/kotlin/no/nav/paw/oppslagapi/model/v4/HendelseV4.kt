package no.nav.paw.oppslagapi.model.v4

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.paw.oppslagapi.model.v3.HendelseType

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PeriodeStartetV4::class, name = "PERIODE_STARTET_V1"),
    JsonSubTypes.Type(value = PeriodeAvluttetV4::class, name = "PERIODE_AVSLUTTET_V1"),
    JsonSubTypes.Type(value = OpplysningerOmArbeidssoekerV4::class, name = "OPPLYSNINGER_V4"),
    JsonSubTypes.Type(value = ProfileringV4::class, name = "PROFILERING_V1"),
    JsonSubTypes.Type(value = EgenvurderingV4::class, name = "EGENVURDERING_V1"),
    JsonSubTypes.Type(value = BekreftelseV4::class, name = "BEKREFTELSE_V1"),
    JsonSubTypes.Type(value = PaaVegneAvStartV4::class, name = "PAA_VEGNE_AV_START_V1"),
    JsonSubTypes.Type(value = PaaVegneAvStoppV4::class, name = "PAA_VEGNE_AV_STOPP_V1")
)
sealed interface HendelseV4 {

    val type: HendelseType
}