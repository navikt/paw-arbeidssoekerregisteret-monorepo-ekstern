package no.nav.paw.oppslagapi.test

import io.mockk.coEvery
import io.mockk.every
import no.nav.paw.error.model.Data
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.model.Info
import no.nav.paw.kafkakeygenerator.model.KafkaKeysInfoResponse
import no.nav.paw.kafkakeygenerator.model.PdlData
import no.nav.paw.kafkakeygenerator.model.PdlId
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.query.DatabaseQuerySupport
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import java.util.*

fun DatabaseQuerySupport.configureMock(
    data: List<Pair<Identitetsnummer, List<Pair<UUID, List<Row<Any>>>>>>
) {
    data.forEach { (identitetsnummer, dataByUser) ->
        every { hentPerioder(identitetsnummer) } returns dataByUser.map { it.first }
        dataByUser.forEach { (periodeId, rows) ->
            every { hentRaderForPeriode(periodeId) } returns rows
        }
    }
}

fun KafkaKeysClient.configureMock(
    brukere: List<Sluttbruker> = TestData.brukere
) {
    brukere.forEach { bruker ->
        bruker.alleIdenter.forEach { identitet ->
            coEvery { this@configureMock.getInfo(identitet.value) } returns KafkaKeysInfoResponse(
                info = Info(
                    identitetsnummer = identitet.value,
                    lagretData = null,
                    pdlData = PdlData(
                        error = null,
                        id = bruker.alleIdenter.map {
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

fun TilgangsTjenesteForAnsatte.configureMock(
    tilgangsConfig: List<TilgangsConfig> = TestData.tilgangsConfig
) {
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







