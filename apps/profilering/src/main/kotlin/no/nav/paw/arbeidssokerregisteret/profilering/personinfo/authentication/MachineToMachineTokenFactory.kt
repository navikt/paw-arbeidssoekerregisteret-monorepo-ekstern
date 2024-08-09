package no.nav.paw.arbeidssokerregisteret.profilering.personinfo.authentication

fun interface MachineToMachineTokenFactory {
    fun create(scope: String): String
}