package no.nav.paw.test.data.periode

import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import java.time.Instant

class TidspunktFraKildeFactory private constructor() {

    fun build(
        tidspunkt: Instant = Instant.now(),
        avviksType: AvviksType = AvviksType.UKJENT_VERDI
    ) = TidspunktFraKilde(tidspunkt, avviksType)

    companion object {
        fun create() = TidspunktFraKildeFactory()
    }
}