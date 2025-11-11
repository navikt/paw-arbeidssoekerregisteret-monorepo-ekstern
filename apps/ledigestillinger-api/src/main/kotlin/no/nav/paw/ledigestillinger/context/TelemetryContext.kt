package no.nav.paw.ledigestillinger.context

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.MultiGauge.Row
import io.micrometer.core.instrument.Tags
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.naw.paw.ledigestillinger.model.Fylke
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*


class TelemetryContext(
    private val meterRegistry: MeterRegistry
) {
    fun finnStillingerByUuidListe(
        uuidListe: Collection<UUID>
    ) {
        Span.current().addEvent(
            "paw.stillinger.event.finn_by_uuid_liste",
            Attributes.of(
                stringKey("uuidListe"), uuidListe.size.toString()
            )
        )
        Counter.builder("paw.stillinger.counter.finn_by_uuid_liste")
            .baseUnit("stillinger")
            .register(meterRegistry)
            .increment()
    }

    fun finnStillingerByEgenskaper(
        soekeord: Collection<String>,
        styrkkoder: Collection<String>,
        fylker: Collection<Fylke>,
    ) {
        Span.current().addEvent(
            "paw.stillinger.event.finn_by_egenskaper",
            Attributes.of(
                stringKey("soekeord"), soekeord.size.toString(),
                stringKey("styrkkoder"), styrkkoder.size.toString(),
                stringKey("fylker"), fylker.size.toString(),
                stringKey("kommuner"), fylker.sumOf { it.kommuner.size }.toString(),
            )
        )
        Counter.builder("paw.stillinger.counter.finn_by_egenskaper")
            .baseUnit("stillinger")
            .register(meterRegistry)
            .increment()
    }

    fun meldingerMottatt(
        antallMottatt: Number,
        antallLagret: Number,
        millisekunder: Number
    ) {
        Span.current().addEvent(
            "paw.stillinger.event.meldinger_mottatt",
            Attributes.of(
                stringKey("mottatt"), antallMottatt.toString(),
                stringKey("lagret"), antallLagret.toString(),
                stringKey("millisekunder"), millisekunder.toString(),
            )
        )
        Counter.builder("paw.stillinger.counter.meldinger_mottatt")
            .baseUnit("stillinger")
            .register(meterRegistry)
            .increment(antallMottatt.toDouble())
    }

    fun lagredeStillinger() = transaction {
        val stillingerGauge = MultiGauge.builder("paw.stillinger.gauge.database")
            .description("Antall stillinger per status i databasen")
            .baseUnit("stillinger")
            .register(meterRegistry)
        val stillingStatusRows = StillingerTable.countByStatus()
        val statusRows = stillingStatusRows.map { (status, antall) ->
            Row.of(Tags.of("status", status.name), antall)
        }
        stillingerGauge.register(statusRows, true)
    }
}