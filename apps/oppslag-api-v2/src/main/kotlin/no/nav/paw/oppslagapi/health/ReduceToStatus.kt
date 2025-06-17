package no.nav.paw.oppslagapi.health

fun Collection<Status>.reduceToStatus(): Status {
    val notOk = filterIsInstance<NotOk>()
    if (notOk.isEmpty()) {
        return Status.OK
    } else {
        val statusFunction: (String) -> Status = if (notOk.any { it is Status.ERROR }) {
            { msg -> Status.ERROR(msg) }
        } else {
            { msg -> Status.PENDING(msg) }
        }
        return statusFunction(
            notOk.joinToString(separator = " | ") { it.message }
        )
    }
}