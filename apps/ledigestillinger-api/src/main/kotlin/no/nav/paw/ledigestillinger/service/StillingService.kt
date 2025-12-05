package no.nav.paw.ledigestillinger.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.context.TelemetryContext
import no.nav.paw.ledigestillinger.exception.StillingIkkeFunnetException
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.asStillingRow
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgivereTable
import no.nav.paw.ledigestillinger.model.dao.EgenskaperTable
import no.nav.paw.ledigestillinger.model.dao.KategorierTable
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringerTable
import no.nav.paw.ledigestillinger.model.dao.LokasjonerTable
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.util.fromLocalDateTimeString
import no.nav.paw.logging.logger.buildLogger
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.Stilling
import no.naw.paw.ledigestillinger.model.StillingStatus
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture.runAsync

class StillingService(
    private val applicationConfig: ApplicationConfig,
    private val telemetryContext: TelemetryContext
) {
    private val logger = buildLogger

    init {
        logger.info(
            "Starter konsumering av stillinger som er publisert etter {}",
            applicationConfig.velgStillingerNyereEnn
        )
    }

    @WithSpan("paw.stillinger.service.hent_by_uuid")
    fun hentStilling(uuid: UUID): Stilling = transaction {
        logger.info("Henter stilling")
        val row = StillingerTable.selectRowByUUID(uuid)
        row?.asDto() ?: throw StillingIkkeFunnetException()
    }

    @WithSpan("paw.stillinger.service.finn_by_uuid_liste")
    fun finnStillingerByUuidListe(
        uuidListe: Collection<UUID>
    ): List<Stilling> = transaction {
        logger.info("Finner stillinger for UUID-liste")
        telemetryContext.finnStillingerByUuidListe(uuidListe)
        val rows = StillingerTable.selectRowsByUUIDList(uuidListe)
        rows.map { it.asDto() }
    }

    @WithSpan("paw.stillinger.service.finn_by_egenskaper")
    fun finnStillingerByEgenskaper(
        medSoekeord: Collection<String>,
        medStyrkkoder: Collection<String>,
        medFylker: Collection<Fylke>,
        paging: Paging = Paging()
    ): List<Stilling> = transaction {
        logger.info("Finner stillinger for egeneskaper")
        telemetryContext.finnStillingerByEgenskaper(medSoekeord, medStyrkkoder, medFylker)
        val rows = StillingerTable.selectRowsByKategorierAndFylker(
            medSoekeord = medSoekeord,
            medStyrkkoder = medStyrkkoder,
            medFylker = medFylker,
            ikkeMedKilder = listOf("DIR"), // TODO Filtrerer ut direktemeldte stillinger
            paging = paging
        )
        rows.map { it.asDto() }
    }

    @WithSpan("paw.stillinger.service.lagre")
    fun lagreStilling(stillingRow: StillingRow) {
        logger.debug("Lagrer stilling")
        val existingId = StillingerTable.selectIdByUUID(stillingRow.uuid)
        if (existingId == null) {
            val id = StillingerTable.insert(stillingRow)
            stillingRow.arbeidsgiver?.let { row ->
                ArbeidsgivereTable.insert(
                    parentId = id,
                    row = row
                )
            }
            KategorierTable.insert(
                parentId = id,
                rows = stillingRow.kategorier
            )
            KlassifiseringerTable.insert(
                parentId = id,
                rows = stillingRow.klassifiseringer
            )
            LokasjonerTable.insert(
                parentId = id,
                rows = stillingRow.lokasjoner
            )
            EgenskaperTable.insert(
                parentId = id,
                rows = stillingRow.egenskaper
            )
        } else {
            StillingerTable.updateById(
                id = existingId,
                row = stillingRow
            )
        }
    }

    @WithSpan("paw.stillinger.service.slett")
    fun slettGamleStillinger() = runCatching {
        val slettEldreEnn = applicationConfig.slettIkkeAktiveStillingerMedUtloeperEldreEnn
        val slettEldreEnnTimestamp = Instant.now()
            .minus(slettEldreEnn)
        logger.info(
            "Sletter ikke-aktive stillinger med utløp eldre enn {} dager ({})",
            slettEldreEnn.toDays(),
            slettEldreEnnTimestamp.truncatedTo(ChronoUnit.SECONDS)
        )

        val future = runAsync {
            val rowsAffected = transaction {
                StillingerTable.deleteByStatusListAndUtloeperLessThan(
                    statusList = listOf(
                        StillingStatus.AVVIST,
                        StillingStatus.INAKTIV,
                        StillingStatus.STOPPET,
                        StillingStatus.SLETTET
                    ),
                    utloeperTimestamp = slettEldreEnnTimestamp
                )
            }

            logger.info(
                "Slettet {} ikke-aktive stillinger med utløp eldre enn {} dager ({})",
                rowsAffected,
                slettEldreEnn.toDays(),
                slettEldreEnnTimestamp.truncatedTo(ChronoUnit.SECONDS)
            )
        }

        logger.info("Venter på fremtiden!!!!")

        future.get(1, java.util.concurrent.TimeUnit.MINUTES)

        logger.info("Fremtiden kom og gikk!!!")
    }.recover { cause ->
        logger.error("Feil ved sletting av utløpte stillinger", cause)
    }.getOrThrow()

    @WithSpan("paw.stillinger.service.handle_messages")
    fun handleMessages(messages: Sequence<Message<UUID, Ad>>): Unit = transaction {
        var antallMottatt = 0
        var antallLagret = 0
        val start = System.currentTimeMillis()
        messages
            .onEach { message ->
                antallMottatt++
                logger.trace("Mottatt melding på topic=${message.topic}, partition=${message.partition}, offset=${message.offset}")
            }
            .filter { message ->
                val publishedTimestamp = message.value.published.fromLocalDateTimeString()
                publishedTimestamp.isAfter(applicationConfig.velgStillingerNyereEnn)
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
            applicationConfig.pamStillingerKafkaConsumer.topic
        )
        telemetryContext.meldingerMottatt(antallMottatt, antallLagret, millisekunder)
    }
}