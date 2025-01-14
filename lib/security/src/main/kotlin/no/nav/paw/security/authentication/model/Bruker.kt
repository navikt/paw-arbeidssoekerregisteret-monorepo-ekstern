package no.nav.paw.security.authentication.model

import java.util.*
import no.nav.paw.model.Identitetsnummer

sealed class Bruker<ID : Any>(
    open val ident: ID
)

data class Sluttbruker(
    override val ident: Identitetsnummer,
    val alleIdenter: Set<Identitetsnummer> = hashSetOf(ident),
) : Bruker<Identitetsnummer>(ident)

data class NavAnsatt(val oid: UUID, override val ident: String) : Bruker<String>(ident)

data class Anonym(val oid: UUID? = null) : Bruker<String>("N/A")
