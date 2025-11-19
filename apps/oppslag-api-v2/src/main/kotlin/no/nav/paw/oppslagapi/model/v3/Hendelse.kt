package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PeriodeStartet::class, name = "PERIODE_STARTET_V1"),
    JsonSubTypes.Type(value = PeriodeAvluttet::class, name = "PERIODE_AVSLUTTET_V1"),
    JsonSubTypes.Type(value = OpplysningerOmArbeidssoeker::class, name = "OPPLYSNINGER_V4"),
    JsonSubTypes.Type(value = Profilering::class, name = "PROFILERING_V1"),
    JsonSubTypes.Type(value = Egenvurdering::class, name = "EGENVURDERING_V1"),
    JsonSubTypes.Type(value = Bekreftelse::class, name = "BEKREFTELSE_V1"),
    JsonSubTypes.Type(value = PaaVegneAvStart::class, name = "PAA_VEGNE_AV_START_V1"),
    JsonSubTypes.Type(value = PaaVegneAvStopp::class, name = "PAA_VEGNE_AV_STOPP_V1")
)
sealed interface Hendelse {

    val type: HendelseType
}