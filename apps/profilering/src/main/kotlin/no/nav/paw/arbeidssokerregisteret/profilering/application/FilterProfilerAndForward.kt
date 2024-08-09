package no.nav.paw.arbeidssokerregisteret.profilering.application

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.helpers.v4.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.profiler
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.sendtInnAvVeilarb
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTopic
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTopicSerde
import no.nav.paw.arbeidssokerregisteret.profilering.utils.Quadruple
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("applicationTopology")

fun KStream<Long, TopicsJoin>.filterProfileAndForward(
    personInfoTjeneste: PersonInfoTjeneste,
    applicationConfiguration: ApplicationConfiguration,
    prometheusRegistry: PrometheusMeterRegistry
) {
    val branches = filter { key, topicsJoins ->
        topicsJoins.isComplete().also { complete ->
            if (!complete) {
                logger.error(
                    "Mangler enten periode eller opplysninger om arbeidssøker, denne skulle ikke vært videresendt: key={}",
                    key
                )
            }
        }
    }.mapValues { _, topicsJoins ->
        val periode = topicsJoins.periode
        val opplysninger = topicsJoins.opplysningerOmArbeidssoeker
        val personInfo = personInfoTjeneste.hentPersonInfo(periode.identitetsnummer, opplysninger.id)
        personInfo to opplysninger
    }
        .mapValues { _, (personInfo, opplysninger) ->
            Quadruple(
                personInfo,
                opplysninger.sendtInnAvVeilarb(),
                profiler(personInfo, opplysninger, veilarbModus = false),
                profiler(personInfo, opplysninger, veilarbModus = true)
            )
        }
        .peek { _, (_, profilertMedVeilarb, stdProfilering, veilarbProfilering) ->
            prometheusRegistry.counter(
                METRICS_PROFILERING,
                Tags.of(
                    Tag.of(LABEL_PROFILERT_TIL, stdProfilering.profilertTil.name),
                    Tag.of(LABEL_VEILARB_PROFILERT_TIL, veilarbProfilering.profilertTil.name),
                    Tag.of(LABEL_PROFILERT_MED, if (profilertMedVeilarb) "veilarb" else "std")
                )
            ).increment()
        }
        .mapValues { _, (personInfo, sendtInnAvVeilarb, stdProfilering, veilarbProfilering) ->
            personInfo to (if (sendtInnAvVeilarb) veilarbProfilering else stdProfilering)
        }

    branches
        .mapValues { _, value -> value.second }
        .filter { _, value -> value is Profilering }
        .process(ProcessorSupplier { TimestampProcessor<Long, Profilering>() })
        .to(applicationConfiguration.profileringTopic)

    branches
        .mapValues { _, (personInfo, profilering) ->
            PersonInfoTopic(profileringId = profilering.id, personInfo = personInfo)
        }
        .process(ProcessorSupplier { TimestampProcessor<Long, PersonInfoTopic>() })
        .to(applicationConfiguration.profileringGrunnlagTopic, Produced.with(Serdes.Long(), PersonInfoTopicSerde()))
}