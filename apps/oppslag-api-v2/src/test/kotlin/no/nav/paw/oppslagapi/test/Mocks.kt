package no.nav.paw.oppslagapi.test

import io.mockk.coEvery
import no.nav.paw.error.model.Data
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.model.Info
import no.nav.paw.kafkakeygenerator.model.KafkaKeysInfoResponse
import no.nav.paw.kafkakeygenerator.model.PdlData
import no.nav.paw.kafkakeygenerator.model.PdlId
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte


fun TilgangsTjenesteForAnsatte.configureMock() {
    TestData.tilgangsConfig.forEach { config ->
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
    TestData.personer.forEach { person ->
        person.forEach { identitet ->
            coEvery { this@configureMock.getInfo(identitet.value) } returns KafkaKeysInfoResponse(
                info = Info(
                    identitetsnummer = identitet.value,
                    lagretData = null,
                    pdlData = PdlData(
                        error = null,
                        id = person.map {
                            PdlId(
                                gruppe = "FOLKEREGISTERIDENT",
                                id = it.value,
                                gjeldende = false
                            )
                        }
                    )
                )
            )
        }
    }
}







