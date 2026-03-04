package no.naw.paw.minestillinger.brukerprofil.direktemeldte

import io.ktor.client.HttpClient
import no.naw.paw.minestillinger.appLogger

fun createDirekteMeldteStillinger(
    direkteMeldteStillingerConfig: DirekteMeldteStillingerConfig,
    getM2MToken: (String) -> String,
    httpClient: HttpClient
): DirekteMeldteStillinger {
    return when (direkteMeldteStillingerConfig.funksjonsnivaa) {
        DirekteMeldteStillingerFunksjonsnivaa.BARE_BRUKERPROFIL,
        DirekteMeldteStillingerFunksjonsnivaa.AKTIVT -> {
        appLogger.info("Oppretter fullverdig DirekteMeldteStillinger basert på funksjonsnivaa ${direkteMeldteStillingerConfig.funksjonsnivaa}")
            DirekteMeldteStillingerHttpClient(
                httpClient = httpClient,
                getM2MToken = getM2MToken,
                config = direkteMeldteStillingerConfig
            )
        }
        DirekteMeldteStillingerFunksjonsnivaa.DEAKTIVERT -> {
            appLogger.info("Vil aldri vise direktemeldte stilinger basert på funksjonsnivaa ${direkteMeldteStillingerConfig.funksjonsnivaa}")
            DirekteMeldteStillingerSkalAldriVises()
        }
    }
}