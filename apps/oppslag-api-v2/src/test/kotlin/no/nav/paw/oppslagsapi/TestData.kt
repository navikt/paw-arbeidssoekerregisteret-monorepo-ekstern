package no.nav.paw.oppslagsapi

import io.mockk.coEvery
import no.nav.paw.error.model.Data
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.model.Info
import no.nav.paw.kafkakeygenerator.model.KafkaKeysInfoResponse
import no.nav.paw.kafkakeygenerator.model.PdlData
import no.nav.paw.kafkakeygenerator.model.PdlId
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.tilgangskontroll.client.Tilgang
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val testLogger: Logger = LoggerFactory.getLogger("no.nav.paw.logger.test")

val person1 = listOf(Identitetsnummer("12345678901"), Identitetsnummer("09876543211"))
val person2 = listOf(Identitetsnummer("12345678902"))

val ansatt1 = NavIdent("Z123456")
val ansatt2 = NavIdent("Z654321")

val tilgangsConfig =
    tilgang(ansatt1, person1, Tilgang.LESE, true) +
            tilgang(ansatt1, person2, Tilgang.LESE, true) +
            tilgang(ansatt2, person1, Tilgang.LESE, false) +
            tilgang(ansatt2, person2, Tilgang.LESE, true)

fun TilgangsTjenesteForAnsatte.configureMock() {
    tilgangsConfig.forEach { config ->
        coEvery {
            this@configureMock.harAnsattTilgangTilPerson(
                navIdent = config.ident,
                identitetsnummer = config.identitetsnummer,
                tilgang = config.tilgangsType
            )
        } returns Data(config.harTilgang)
    }
}

fun KafkaKeysClient.configureMock() {
    listOf(person1, person2).forEach { person ->
        person.forEach { identitet ->
            coEvery { this@configureMock.getInfo(identitet.verdi) } returns KafkaKeysInfoResponse(
                info = Info(
                    identitetsnummer = identitet.verdi,
                    lagretData = null,
                    pdlData = PdlData(
                        error = null,
                        id = person.map {
                            PdlId(
                                gruppe = "FOLKEREGISTERIDENT",
                                id = it.verdi,
                                gjeldende = false
                            )
                        }
                    )
                )
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


