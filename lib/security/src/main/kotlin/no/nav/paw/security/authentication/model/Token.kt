package no.nav.paw.security.authentication.model

sealed class Token(val issuer: Issuer, val claims: List<Claim<*>>)

data object IdPortenToken : Token(IdPorten, listOf(PID))
data object TokenXToken : Token(TokenX, listOf(PID))
data object AzureAdToken : Token(AzureAd, listOf(OID, Name, NavIdent, Roles))

fun getValidTokens(): List<Token> = listOf(IdPortenToken, TokenXToken, AzureAdToken)
