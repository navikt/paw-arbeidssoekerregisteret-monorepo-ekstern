package no.naw.paw.minestillinger.brukerprofil

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ProblemDetailsException
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.naw.paw.minestillinger.brukerprofil.flagg.ErITestGruppenFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBruktTjenestenFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGradertAdresseFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.ListeMedFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.OppdateringAvFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.OptOutFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.ingenOppdateringAvFlagg
import no.naw.paw.minestillinger.domain.TjenesteStatus
import java.time.Instant
import java.time.Instant.now
import java.util.*


fun ListeMedFlagg.tjenestestatus(): TjenesteStatus {
    return when {
        isTrue(TjenestenErAktivFlaggtype) -> TjenesteStatus.AKTIV
        isTrue(HarGradertAdresseFlaggtype) -> TjenesteStatus.KAN_IKKE_LEVERES
        isFalse(ErITestGruppenFlaggtype) -> TjenesteStatus.KAN_IKKE_LEVERES
        isFalse(HarBruktTjenestenFlaggtype) &&
                isFalse(HarGodeMuligheterFlaggtype) -> TjenesteStatus.KAN_IKKE_LEVERES

        isTrue(OptOutFlaggtype) -> TjenesteStatus.OPT_OUT
        else -> TjenesteStatus.INAKTIV
    }
}

fun beregnOppdateringAvFlaggFraAPI(
    tidspunkt: Instant,
    gjeldendeStatus: TjenesteStatus,
    nyTjenestestatus: TjenesteStatus
): OppdateringAvFlagg {
    return when (gjeldendeStatus to nyTjenestestatus) {
        TjenesteStatus.AKTIV to TjenesteStatus.AKTIV -> ingenOppdateringAvFlagg
        TjenesteStatus.AKTIV to TjenesteStatus.INAKTIV -> OppdateringAvFlagg(
            nyeOgOppdaterteFlagg = listOf(
                TjenestenErAktivFlaggtype.flagg(verdi = false, tidspunkt = tidspunkt),
            ),
            søkSkalSlettes = false
        )

        TjenesteStatus.AKTIV to TjenesteStatus.OPT_OUT -> OppdateringAvFlagg(
            nyeOgOppdaterteFlagg = listOf(
                OptOutFlaggtype.flagg(verdi = true, tidspunkt = tidspunkt),
                TjenestenErAktivFlagg(verdi = false, tidspunkt = tidspunkt)
            ),
            søkSkalSlettes = true
        )

        TjenesteStatus.INAKTIV to TjenesteStatus.AKTIV -> OppdateringAvFlagg(
            nyeOgOppdaterteFlagg = listOf(
                TjenestenErAktivFlaggtype.flagg(verdi = true, tidspunkt = tidspunkt),
                HarBruktTjenestenFlaggtype.flagg(verdi = true, tidspunkt = tidspunkt)
            ),
            søkSkalSlettes = false
        )

        TjenesteStatus.INAKTIV to TjenesteStatus.INAKTIV -> ingenOppdateringAvFlagg
        TjenesteStatus.INAKTIV to TjenesteStatus.OPT_OUT -> OppdateringAvFlagg(
            nyeOgOppdaterteFlagg = listOf(
                OptOutFlaggtype.flagg(verdi = true, tidspunkt = tidspunkt)
            ),
            søkSkalSlettes = true
        )

        TjenesteStatus.OPT_OUT to TjenesteStatus.AKTIV -> OppdateringAvFlagg(
            nyeOgOppdaterteFlagg = listOf(
                OptOutFlaggtype.flagg(verdi = false, tidspunkt = tidspunkt),
                TjenestenErAktivFlaggtype.flagg(verdi = true, tidspunkt = tidspunkt)
            ),
            søkSkalSlettes = false
        )

        TjenesteStatus.OPT_OUT to TjenesteStatus.INAKTIV -> OppdateringAvFlagg(
            nyeOgOppdaterteFlagg = listOf(
                OptOutFlaggtype.flagg(verdi = false, tidspunkt = tidspunkt)
            ),
            søkSkalSlettes = false
        )

        TjenesteStatus.OPT_OUT to TjenesteStatus.OPT_OUT -> ingenOppdateringAvFlagg
        else -> ingenOppdateringAvFlagg
    }
}

fun OppdateringAvFlagg.response(): Data<OppdateringAvFlagg> = Data(this)

fun oppdateringIkkeTillatt(detail: String): ProblemDetails {
    return ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType.default().error("oppdatering-ikke-tillatt").domain("mine-stillinger").build(),
        title = "Oppdatering av tjenestestatus ikke tillatt",
        detail = detail,
        status = HttpStatusCode.Forbidden,
        instance = "api_kan_sette_tjenestestatus"
    )
}



