package no.nav.paw.arbeidssoekerregisteret.mapping

import no.nav.paw.arbeidssoekerregisteret.exception.EgenvurderingIkkeStoettetException
import no.nav.paw.arbeidssoekerregisteret.exception.ProfileringIkkeStoettetException
import no.nav.paw.arbeidssoekerregisteret.model.EgenvurdertTil
import no.nav.paw.arbeidssoekerregisteret.model.NyesteProfileringRow
import no.nav.paw.arbeidssoekerregisteret.model.Profilering
import no.nav.paw.arbeidssoekerregisteret.model.ProfilertTil
import java.time.Instant
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil as AvroProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering as AvroEgenvurdering

fun AvroProfilertTil.asDto(): ProfilertTil = when (this) {
    AvroProfilertTil.ANTATT_GODE_MULIGHETER -> ProfilertTil.ANTATT_GODE_MULIGHETER
    AvroProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    AvroProfilertTil.OPPGITT_HINDRINGER -> ProfilertTil.OPPGITT_HINDRINGER
    AvroProfilertTil.UKJENT_VERDI -> ProfilertTil.UKJENT_VERDI
    AvroProfilertTil.UDEFINERT -> ProfilertTil.UDEFINERT
}

fun ProfilertTil.asAvro(): AvroProfilertTil = when (this) {
    ProfilertTil.ANTATT_GODE_MULIGHETER -> AvroProfilertTil.ANTATT_GODE_MULIGHETER
    ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> AvroProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    ProfilertTil.OPPGITT_HINDRINGER -> AvroProfilertTil.OPPGITT_HINDRINGER
    ProfilertTil.UKJENT_VERDI -> AvroProfilertTil.UKJENT_VERDI
    ProfilertTil.UDEFINERT -> AvroProfilertTil.UDEFINERT
}

fun EgenvurdertTil.asAvro(): AvroProfilertTil = when (this) {
    EgenvurdertTil.ANTATT_GODE_MULIGHETER -> AvroProfilertTil.ANTATT_GODE_MULIGHETER
    EgenvurdertTil.ANTATT_BEHOV_FOR_VEILEDNING -> AvroProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    EgenvurdertTil.OPPGITT_HINDRINGER -> AvroProfilertTil.OPPGITT_HINDRINGER
}

fun NyesteProfileringRow.asDto() = Profilering(
    profileringId = id,
    profilertTil = profilertTil
)

fun AvroEgenvurdering.validerEgenvurdering(): AvroEgenvurdering {
    if (profilertTil.erGyldig()) {
        if (egenvurdering.erGyldig()) {
            return this
        } else {
            throw EgenvurderingIkkeStoettetException(profilertTil, egenvurdering)
        }
    } else {
        throw ProfileringIkkeStoettetException(profilertTil, egenvurdering)
    }
}

fun NyesteProfileringRow.erGyldig(prodsettingstidspunkt: Instant): Boolean {
    return profilertTil.erGyldig() && periodeStartetTidspunkt.isAfter(prodsettingstidspunkt)
}

fun ProfilertTil.erGyldig(): Boolean = this == ProfilertTil.ANTATT_GODE_MULIGHETER ||
        this == ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING

fun AvroProfilertTil.erGyldig(): Boolean = this == AvroProfilertTil.ANTATT_GODE_MULIGHETER ||
        this == AvroProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING