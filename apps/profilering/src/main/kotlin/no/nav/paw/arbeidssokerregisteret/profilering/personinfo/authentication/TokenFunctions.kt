package no.nav.paw.arbeidssokerregisteret.profilering.personinfo.authentication

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.cache.CaffeineTokenCache
import no.nav.common.token_client.cache.TokenCache
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun m2mTokenFactory(azureConfig: AzureConfig): MachineToMachineTokenFactory {
    val client = azureAdTokenClient(azureConfig)
    return MachineToMachineTokenFactory { scope ->
        client.createMachineToMachineToken(scope)
    }
}

fun azureAdTokenClient(azureConfig: AzureConfig): AzureAdMachineToMachineTokenClient {
    return when (System.getenv("NAIS_CLUSTER_NAME")) {
        "prod-gcp", "dev-gcp" -> AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .withCache(CaffeineTokenCache())
            .buildMachineToMachineTokenClient()

        else -> AzureAdTokenClientBuilder.builder()
            .withClientId(azureConfig.clientId)
            .withPrivateJwk(createMockRSAKey("azure"))
            .withTokenEndpointUrl(azureConfig.tokenEndpointUrl)
            .buildMachineToMachineTokenClient()
    }
}

fun createMockRSAKey(keyID: String): String? = KeyPairGenerator
    .getInstance("RSA").let {
        it.initialize(2048)
        it.generateKeyPair()
    }.let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keyID)
            .build()
            .toJSONString()
    }
