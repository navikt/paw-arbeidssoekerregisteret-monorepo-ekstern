package no.nav.paw.test.data.periode

import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import java.time.Instant

class MetadataFactory private constructor() {
    fun build(
        tidspunkt: Instant = Instant.now(),
        utfortAv: Bruker = BrukerFactory.create().build(),
        kilde: String = "test-data-factory",
        aarsak: String = "testing",
        tidspunktFraKilde: TidspunktFraKilde = TidspunktFraKildeFactory.create().build()
    ) = Metadata(tidspunkt, utfortAv, kilde, aarsak, tidspunktFraKilde)

    companion object {
        fun create() = MetadataFactory()
    }
}
