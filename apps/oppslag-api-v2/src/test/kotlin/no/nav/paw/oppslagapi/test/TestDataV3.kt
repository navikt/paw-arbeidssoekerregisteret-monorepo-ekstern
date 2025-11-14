package no.nav.paw.oppslagapi.test

import no.nav.paw.felles.model.NavIdent
import no.nav.paw.security.authentication.model.NavAnsatt
import java.util.*

object TestDataV3 {

    val navId1 = NavIdent("Z123456")

    val navAnstatt1 = NavAnsatt(
        oid = UUID.randomUUID(),
        ident = navId1.value,
        sikkerhetsnivaa = "azure:Level4"
    )
}