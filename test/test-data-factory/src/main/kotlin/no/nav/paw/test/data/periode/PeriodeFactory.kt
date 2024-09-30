package no.nav.paw.test.data.periode

import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.util.*

class PeriodeFactory private constructor() {

    fun build(
        id: UUID = UUID.randomUUID(),
        identitetsnummer: String = "01017012345",
        startet: Metadata = MetadataFactory.create().build(),
        avsluttet: Metadata? = null
    ) = Periode(id, identitetsnummer, startet, avsluttet)

    companion object {
        fun create() = PeriodeFactory()
    }
}

