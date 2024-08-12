package no.nav.paw.arbeidssokerregisteret.profilering.application

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.helpers.v4.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import org.apache.kafka.streams.StreamsBuilder
import java.time.Instant
import kotlin.reflect.KClass

class SetupStreams(
    val streamBuilder: StreamsBuilder,
    val personInfoTjeneste: PersonInfoTjeneste,
    val applicationConfiguration: ApplicationConfiguration,
    val prometheusRegistry: PrometheusMeterRegistry
) {
    inline fun <reified T> setupStream(
        topic: String,
        stateStoreSaveClass: KClass<out BaseStateStoreSave>
    ) {
        streamBuilder
            .stream<Long, T>(topic)
            .filter{ _, value -> when(value){
                is Periode -> true
                is OpplysningerOmArbeidssoeker -> value.sendtInnAv.tidspunkt.isAfter(Instant.parse("2024-01-01T00:00:00Z"))
                else -> false
            } }
            .mapValues { _, value -> TopicsJoin(value as? Periode, null, value as? OpplysningerOmArbeidssoeker) }
            .saveAndForwardIfComplete(
                stateStoreSaveClass,
                applicationConfiguration.joiningStateStoreName,
                prometheusRegistry
            )
            .filterProfileAndForward(
                personInfoTjeneste,
                applicationConfiguration,
                prometheusRegistry
            )
    }
}