package no.nav.paw.ledigestillinger.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.context.TelemetryContext
import no.nav.paw.ledigestillinger.exception.StillingIkkeFunnetException
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.asStillingRow
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgivereTableV2
import no.nav.paw.ledigestillinger.model.dao.EgenskaperTableV2
import no.nav.paw.ledigestillinger.model.dao.KategorierTableV2
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringerTableV2
import no.nav.paw.ledigestillinger.model.dao.LokasjonerTableV2
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.model.dao.StillingerTableV2
import no.nav.paw.ledigestillinger.util.skalBeholdes
import no.nav.paw.logging.logger.buildLogger
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.Stilling
import no.naw.paw.ledigestillinger.model.StillingStatus
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class StillingServiceV2(
    private val clock: Clock,
    private val applicationConfig: ApplicationConfig,
    private val telemetryContext: TelemetryContext
) {
    private val logger = buildLogger

    init {
        logger.info(
            "Starter konsumering av stillinger som er publisert etter {}",
            applicationConfig.beholdAlleStillingerPublisertEtter.truncatedTo(ChronoUnit.SECONDS)
        )
    }

    @WithSpan("paw.stillinger.service.hent_by_uuid")
    fun hentStilling(uuid: UUID): Stilling = transaction {
        logger.info("Henter stilling")
        val row = StillingerTableV2.selectRowByUUID(uuid)
        row?.asDto() ?: throw StillingIkkeFunnetException()
    }

    @WithSpan("paw.stillinger.service.finn_by_uuid_liste")
    fun finnStillingerByUuidListe(
        uuidListe: Collection<UUID>
    ): List<Stilling> = transaction {
        val start = System.currentTimeMillis()
        try {
            telemetryContext.tellStillingerByUuidListe(uuidListe)
            val rows = StillingerTableV2.selectRowsByUUIDList(uuidListe)
            rows.map { it.asDto() }
        } finally {
            val slutt = System.currentTimeMillis()
            val millisekunder = slutt - start
            logger.info("Fant stillinger for UUID-liste på {}ms", millisekunder)
        }
    }

    @WithSpan("paw.stillinger.service.finn_by_egenskaper")
    fun finnStillingerByEgenskaper(
        medSoekeord: Collection<String>,
        medStyrkkoder: Collection<String>,
        medFylker: Collection<Fylke>,
        paging: Paging = Paging()
    ): List<Stilling> = transaction {
        val start = System.currentTimeMillis()
        try {
            telemetryContext.tellStillingerByEgenskaper(medSoekeord, medStyrkkoder, medFylker)
            val rows = StillingerTableV2.selectRowsByKategorierAndFylker(
                medSoekeord = medSoekeord,
                medStyrkkoder = medStyrkkoder,
                medFylker = medFylker,
                utenKilder = listOf("DIR"), // TODO Filtrerer ut direktemeldte stillinger
                paging = paging
            )
            rows.map { it.asDto() }
        } finally {
            val slutt = System.currentTimeMillis()
            val millisekunder = slutt - start
            logger.info("Fant stillinger for egeneskaper på {}ms", millisekunder)
        }
    }

    @WithSpan("paw.stillinger.service.lagre")
    fun lagreStilling(stillingRow: StillingRow) {
        logger.debug("Lagrer stilling")
        val existingId = StillingerTableV2.selectIdByUUID(stillingRow.uuid)
        if (existingId == null) {
            val id = StillingerTableV2.insert(stillingRow)
            stillingRow.arbeidsgiver?.let { row ->
                ArbeidsgivereTableV2.insert(
                    parentId = id,
                    row = row
                )
            }
            KategorierTableV2.insert(
                parentId = id,
                rows = stillingRow.kategorier
            )
            KlassifiseringerTableV2.insert(
                parentId = id,
                rows = stillingRow.klassifiseringer
            )
            LokasjonerTableV2.insert(
                parentId = id,
                rows = stillingRow.lokasjoner
            )
            EgenskaperTableV2.insert(
                parentId = id,
                rows = stillingRow.egenskaper
            )
        } else {
            StillingerTableV2.updateById(
                id = existingId,
                row = stillingRow
            )
        }
    }

    @WithSpan("paw.stillinger.service.slett")
    fun slettUtloepteStillinger() = runCatching {
        with(applicationConfig) {
            val beholdNyereEnnTimestamp = Instant.now(clock)
                .minus(beholdIkkeAktiveStillingerMedUtloeperNyereEnn)
            logger.info(
                "Sletter ikke-aktive stillinger med utløp eldre enn {} dager ({})",
                beholdIkkeAktiveStillingerMedUtloeperNyereEnn.toDays(),
                beholdNyereEnnTimestamp.truncatedTo(ChronoUnit.SECONDS)
            )

            val rowsAffected = transaction {
                val count = StillingerTableV2.countByStatus()
                logger.info("Stillinger by count: {}", count)
                StillingerTableV2.deleteByStatusListAndUtloeperLessThan(
                    statusList = listOf(
                        StillingStatus.AVVIST,
                        StillingStatus.INAKTIV,
                        StillingStatus.STOPPET,
                        StillingStatus.SLETTET
                    ),
                    utloeperTimestamp = beholdNyereEnnTimestamp
                )
            }

            logger.info(
                "Slettet {} ikke-aktive stillinger med utløp eldre enn {} dager ({})",
                rowsAffected,
                beholdIkkeAktiveStillingerMedUtloeperNyereEnn.toDays(),
                beholdNyereEnnTimestamp.truncatedTo(ChronoUnit.SECONDS)
            )
        }
    }.recover { cause ->
        logger.error("Feil ved sletting av utløpte stillinger", cause)
    }.getOrThrow()

    @WithSpan("paw.stillinger.service.handle_messages")
    fun handleMessages(messages: Sequence<Message<UUID, Ad>>): Unit = transaction {
        with(applicationConfig) {
            var antallMottatt = 0
            var antallLagret = 0
            val start = System.currentTimeMillis()
            messages
                .onEach { message ->
                    antallMottatt++
                    logger.trace("Mottatt melding på topic=${message.topic}, partition=${message.partition}, offset=${message.offset}")
                }
                .filter { message ->
                    message.skalBeholdes(publisertGrense = beholdAlleStillingerPublisertEtter)
                }
                .onEach { message ->
                    logger.trace("Prosesserer melding på topic=${message.topic}, partition=${message.partition}, offset=${message.offset}")
                }
                .forEach { message ->
                    runCatching {
                        val stillingRow = message.asStillingRow()
                        lagreStilling(stillingRow)
                        antallLagret++
                    }.onFailure { cause ->
                        logger.error("Feil ved mottak av melding", cause)
                    }.getOrThrow()
                }
            val slutt = System.currentTimeMillis()
            val millisekunder = slutt - start
            logger.info(
                "Håndterte {} meldinger på {}ms fra topic={}",
                antallMottatt,
                millisekunder,
                pamStillingerKafkaConsumer.topic
            )
            telemetryContext.tellMeldingerMottatt(antallMottatt, antallLagret, millisekunder)
        }
    }
}