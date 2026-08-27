package no.naw.paw.minestillinger

import kotlinx.coroutines.sync.Mutex
import java.time.Duration

class Vedlikeholdslås {
    private val mutex = Mutex()

    suspend fun <T> kjørEksklusivt(jobbnavn: String, block: suspend () -> T): T {
        val ventetFra = System.nanoTime()
        val måtteVente = !mutex.tryLock()
        if (måtteVente) {
            appLogger.info("Bakgrunnsjobb venter på kjøretillatelse: $jobbnavn")
            mutex.lock()
        }
        return try {
            if (måtteVente) {
                appLogger.info(
                    "Bakgrunnsjobb fikk kjøretillatelse: {}, ventet {} ms",
                    jobbnavn,
                    Duration.ofNanos(System.nanoTime() - ventetFra).toMillis()
                )
            }
            block()
        } finally {
            mutex.unlock()
        }
    }
}
