package no.nav.paw.arbeidssoekerregisteret.model

import java.util.*

data class NavAnsatt(val azureId: UUID, val navIdent: String)

data class Claim(
    val name: String,
    val value: String
)

val Claim.isPid get(): Boolean = this.name == "pid"
val Claim.isOid get(): Boolean = this.name == "oid"
val Claim.isNavIdent get(): Boolean = this.name == "NAVident"

data class ResolvedClaim(
    val issuer: String,
    val claim: Claim
)

val ResolvedClaim.isTokenx get(): Boolean = this.issuer == "tokenx"
val ResolvedClaim.isAzure get(): Boolean = this.issuer == "azure"

typealias ResolvedClaims = List<ResolvedClaim>

val ResolvedClaims.isTokenx get(): Boolean = any { it.isTokenx }
val ResolvedClaims.isAzure get(): Boolean = any { it.isAzure }

fun ResolvedClaims.getPid(): String? = find { it.isTokenx && it.claim.isPid }?.claim?.value

fun ResolvedClaims.getOid(): UUID? =
    find { it.isAzure && it.claim.isOid }?.claim?.value?.let(java.util.UUID::fromString)

fun ResolvedClaims.getNAVident(): String? = find { it.isAzure && it.claim.isNavIdent }?.claim?.value

val claimsList = listOf(
    "tokenx" to "pid"
)
