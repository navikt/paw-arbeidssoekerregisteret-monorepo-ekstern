package no.naw.paw.ledigestillinger.model

import io.kotest.core.spec.style.FreeSpec
import no.naw.paw.ledigestillinger.ledigeStillingerApiObjectMapper
import java.util.*

class FinnStillingerRequestTest : FreeSpec({
    "Serde test" {
        val request1 = FinnStillingerByEgenskaperRequest(
            type = FinnStillingerType.BY_EGENSKAPER,
            soekeord = emptyList(),
            kategorier = listOf("1234", "5678"),
            fylker = listOf(
                Fylke(
                    fylkesnummer = "03",
                    kommuner = emptyList()
                )
            ),
            paging = Paging(1, 10)
        )
        val request3 = FinnStillingerByUuidListeRequest(
            type = FinnStillingerType.BY_UUID_LISTE,
            uuidListe = listOf(UUID.randomUUID(), UUID.randomUUID())
        )

        val serialized1 = ledigeStillingerApiObjectMapper.writeValueAsString(request1)
        val serialized3 = ledigeStillingerApiObjectMapper.writeValueAsString(request3)

        val deserialized1 = ledigeStillingerApiObjectMapper.readValue(serialized1, FinnStillingerRequest::class.java)
        val deserialized3 = ledigeStillingerApiObjectMapper.readValue(serialized3, FinnStillingerRequest::class.java)
    }
})