package no.nav.paw.oppslagapi.health

fun healthIndicator(
    isAlive: List<IsAlive> = emptyList(),
    isReady: List<IsReady> = emptyList(),
    hasStarted: List<HasStarted> = emptyList()
): HealthIndicator {
    return CompoudHealthIndicator(isAlive, isReady, hasStarted)
}

private class CompoudHealthIndicator(
    private val _isAlive: List<IsAlive>,
    private val _isReady: List<IsReady>,
    private val _hasStarted: List<HasStarted>
): HealthIndicator {

    override val isAlive: Status
        get() = _isAlive.map(IsAlive::invoke).reduceToStatus()

    override val isReady: Status
        get() = _isReady.map(IsReady::invoke).reduceToStatus()

    override val hasStarted: Status
        get() = _hasStarted.map(HasStarted::invoke).reduceToStatus()
}