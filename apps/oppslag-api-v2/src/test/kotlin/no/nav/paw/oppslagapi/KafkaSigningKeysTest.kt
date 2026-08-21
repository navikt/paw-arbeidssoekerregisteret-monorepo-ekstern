package no.nav.paw.oppslagapi

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import no.nav.paw.kafka.signing.loadPublicKeysFromClasspath

class KafkaSigningKeysTest : FreeSpec({
    val signerByTopic = mapOf(
        "periode" to "paw-event-processor",
        "opplysninger" to "paw-event-processor",
        "profilering" to "paw-profilering",
        "egenvurdering" to "paw-egenvurdering-api",
        "bekreftelse" to "paw-api-bekreftelse",
        "paa-vegne-av" to "paw-bekreftelse-filter",
    )

    "har offentlige dev- og prod-nøkler for alle signerte topics" {
        val expectedKeyIds = signerByTopic.values
            .filterNotNull()
            .flatMap { signer -> listOf("dev-$signer-ecdsa-v1", "prod-$signer-ecdsa-v1") }
            .toSet()

        loadPublicKeysFromClasspath().keys shouldContainAll expectedKeyIds
    }
})
