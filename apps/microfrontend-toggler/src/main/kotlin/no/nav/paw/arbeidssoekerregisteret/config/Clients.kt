package no.nav.paw.arbeidssoekerregisteret.config

import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.arbeidssoekerregisteret.exception.PdlClientException
import no.nav.paw.arbeidssoekerregisteret.model.AdTema
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.kafkakeygenerator.client.kafkaKeysKlient
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.PdlException
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.pdl.hentIdenter
import java.util.*

private const val consumerId = "paw-arbeidssoekerregisteret"

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse? = runBlocking {
    getIdAndKey(identitetsnummer)
}

fun PdlClient.hentFolkeregisterIdentBlocking(ident: String): IdentInformasjon? {
    return try {
        val identer = hentIdenterBlocking(ident)
        if (identer.isNullOrEmpty()) return null
        identer.first { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }
    } catch (e: PdlException) {
        if (e.errors?.any { it.message.contains("Fant ikke person") } == true) {
            return null
        } else {
            throw PdlClientException("Kall til PDL feilet", e)
        }
    } catch (e: Exception) {
        throw PdlClientException("Ukjent feil ved kall til PDL", e)
    }
}

private fun PdlClient.hentIdenterBlocking(ident: String): List<IdentInformasjon>? = runBlocking {
    hentIdenter(
        ident = ident,
        callId = UUID.randomUUID().toString(),
        navConsumerId = consumerId
    )
}

fun buildKafkaKeysClient(
    kafkaKeyConfig: KafkaKeyConfig, azureM2MTokenClient: AzureAdMachineToMachineTokenClient
) = kafkaKeysKlient(kafkaKeyConfig) {
    azureM2MTokenClient.createMachineToMachineToken(kafkaKeyConfig.scope)
}

fun buildPdlClient(
    serviceClientConfig: ServiceClientConfig, azureM2MTokenClient: AzureAdMachineToMachineTokenClient
) = PdlClient(
    serviceClientConfig.url, AdTema.OPP.value, HttpClient()
) {
    azureM2MTokenClient.createMachineToMachineToken(serviceClientConfig.scope)
}
