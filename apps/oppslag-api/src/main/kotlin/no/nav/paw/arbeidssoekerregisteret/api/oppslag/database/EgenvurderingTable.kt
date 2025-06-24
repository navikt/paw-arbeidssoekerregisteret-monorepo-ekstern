package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PGEnum
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import org.jetbrains.exposed.dao.id.LongIdTable

object EgenvurderingTable : LongIdTable("egenvurdering") {
    val egenvurderingId = uuid("egenvurdering_id")
    val periodeId = uuid("periode_id")
    val opplysningerOmArbeidssoekerId = uuid("opplysninger_om_arbeidssoeker_id")
    val profileringId = uuid("profilering_id")
    val sendtInnAvId = long("sendt_inn_av_id").references(MetadataTable.id)
    val egenvurdering =
        customEnumeration(
            "egenvurdering",
            "ProfilertTil",
            { value -> ProfilertTil.valueOf(value as String) },
            { PGEnum("ProfilertTil", it) }
        )
}