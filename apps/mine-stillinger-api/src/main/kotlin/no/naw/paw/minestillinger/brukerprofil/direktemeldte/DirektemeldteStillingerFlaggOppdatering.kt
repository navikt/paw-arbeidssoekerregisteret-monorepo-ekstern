package no.naw.paw.minestillinger.brukerprofil.direktemeldte

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.delay
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.ReadinessCheck
import no.nav.paw.health.StartupCheck
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.flagg.InkluderDirekteMeldteStillingerFlagtype
import no.naw.paw.minestillinger.brukerprofil.flagg.InkluderDirekteMeldteStillingerFlagg
import no.naw.paw.minestillinger.db.ops.hentAlleAktiveBrukereMedUtdatertFlagg
import no.naw.paw.minestillinger.db.ops.hentAlleAktiveBrukereSomManglerFlagg
import no.naw.paw.minestillinger.db.ops.skrivFlaggTilDB
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class DirektemeldteStillingerFlaggOppdatering(
    private val direktemeldteStillingerTilgangClient: DirektemeldteStillingerTilgangClient,
    private val clock: Clock,
    private val oppdateringsintervall: Duration,
    private val gyldighetsperiode: Duration
) : LivenessCheck, ReadinessCheck, StartupCheck, Closeable {
    private val hasStarted = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)
    private val continueRunning = AtomicBoolean(true)

    suspend fun start() {
        if (hasStarted.compareAndSet(false, true)) {
            try {
                isRunning.set(true)
                while (continueRunning.get()) {
                    oppdaterFlaggForDirektemeldteStillinger()
                    if (continueRunning.get()) {
                        delay(timeMillis = oppdateringsintervall.toMillis())
                    }
                }
            } finally {
                isRunning.set(false)
            }
        } else {
            appLogger.warn("Kan ikke starte oppdatering av direktemeldte stillinger flere ganger")
        }
    }

    @WithSpan("vedlikehold_oppdater_direktemeldte_stillinger_flagg")
    suspend fun oppdaterFlaggForDirektemeldteStillinger() {
        val alleMedUtdaterteFlagg = transaction {
            val utdatert = hentAlleAktiveBrukereMedUtdatertFlagg(
                alleFraFørDetteErUtløpt = clock.now() - gyldighetsperiode,
                flaggtype = InkluderDirekteMeldteStillingerFlagtype,
                limit = 100
            )
            val manglende = hentAlleAktiveBrukereSomManglerFlagg(
                flaggtype = InkluderDirekteMeldteStillingerFlagtype,
                limit = 100
            )
            (utdatert + manglende).distinctBy { it.id }
        }
        appLogger.info("${alleMedUtdaterteFlagg.size} brukere med utdaterte flagg for direktemeldte stillinger")
        val oppdateringer = alleMedUtdaterteFlagg.map { profile ->
            val harTilgang = direktemeldteStillingerTilgangClient.skalSeDirektemeldteStillinger(profile.identitetsnummer)
            profile.id to InkluderDirekteMeldteStillingerFlagg(
                verdi = harTilgang,
                tidspunkt = clock.now()
            )
        }.toList()
        transaction {
            oppdateringer.forEach { (brukerId, flagg) ->
                skrivFlaggTilDB(brukerId, listOf(flagg))
            }
        }
    }

    override fun hasStarted(): Boolean {
        return hasStarted.get()
    }

    override fun isAlive(): Boolean {
        return isRunning.get()
    }

    override fun isReady(): Boolean {
        return isRunning.get()
    }

    override fun close() {
        continueRunning.set(false)
    }

}
