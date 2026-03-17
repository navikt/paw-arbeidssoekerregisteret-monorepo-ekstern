package no.naw.paw.minestillinger.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.common.AttributeKey.booleanKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.ledigestillinger.model.FinnStillingerByEgenskaperRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerType
import no.naw.paw.ledigestillinger.model.Frist
import no.naw.paw.ledigestillinger.model.FristType
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Kommune
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.Sektor
import no.naw.paw.ledigestillinger.model.SortOrder
import no.naw.paw.ledigestillinger.model.Stilling
import no.naw.paw.ledigestillinger.model.Tag
import no.naw.paw.minestillinger.ArbeidsplassenMapper
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.FinnStillingerClient
import no.naw.paw.minestillinger.api.ApiJobbAnnonse
import no.naw.paw.minestillinger.api.MineStillingerResponse
import no.naw.paw.minestillinger.api.Soeknadsfrist
import no.naw.paw.minestillinger.api.SoeknadsfristType
import no.naw.paw.minestillinger.api.vo.ApiSortOrder
import no.naw.paw.minestillinger.api.vo.ApiTag
import no.naw.paw.minestillinger.api.vo.toApiTag
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.flagg.InkluderDirekteMeldteStillingerFlagtype
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.LagretStillingsoek
import no.naw.paw.minestillinger.domain.StedSoek
import no.naw.paw.minestillinger.domain.SøkId
import no.naw.paw.minestillinger.domain.api
import no.naw.paw.minestillinger.metrics.tellLedigeStillingerKall
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

const val MINE_LEDIGE_STILLINGER_PATH = "/api/v1/ledigestillinger"

fun Route.ledigeStillingerRoute(
    meterRegistry: MeterRegistry,
    ledigeStillingerClient: FinnStillingerClient,
    hentBrukerProfil: suspend (Identitetsnummer) -> BrukerProfil?,
    hentLagretSøk: (BrukerId) -> List<LagretStillingsoek>,
    oppdaterSistKjøt: (SøkId, Instant) -> Boolean,
    clock: Clock
) {
    route(MINE_LEDIGE_STILLINGER_PATH) {
        autentisering(TokenX) {
            get {
                val identitetsnummer = call.securityContext()
                    .hentSluttbrukerEllerNull()
                    ?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val søkOgRequest: Pair<LagretStillingsoek, List<FinnStillingerRequest>>? = suspendTransaction {
                    val bruker = hentBrukerProfil(identitetsnummer)
                    val brukerId = bruker?.id
                    val soek = brukerId?.let { id -> hentLagretSøk(id) }
                        ?.firstOrNull { it.soek is StedSoek }
                    soek?.let { stedSøk ->
                        val søk = stedSøk.soek as StedSoek
                        val page = call.request.queryParameters["page"]?.toInt() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 10
                        val sort = call.request.queryParameters["sort"]?.let(ApiSortOrder::valueOf) ?: ApiSortOrder.DESC
                        if (page < 1) throw BadRequestException("Parameter 'page' må være 1 eller større")
                        if (pageSize !in 1..100) throw BadRequestException("Parameter 'pageSize' må være mellom 1 og 100")
                        val direkteMeldingerSøk = if (bruker.listeMedFlagg.isTrue(InkluderDirekteMeldteStillingerFlagtype)) {
                            Span.current().addEvent("direktemeldte_stillinger", Attributes.of(booleanKey("inkluder"), true))
                            genererRequest(søk = søk.copy(
                                fylker = emptyList(),
                                soekeord = emptyList(),
                                styrk08 = søk.styrk08.flatMap { ArbeidsplassenMapper.relaterteStyrkKoder(it) }.distinct(),
                            ), page = page, pageSize = pageSize, sort = sort, listOf(Tag.DIREKTEMELDT_V1))
                        } else {
                            Span.current().addEvent("direktemeldte_stillinger", Attributes.of(booleanKey("inkluder"), false))
                            null
                        }
                        stedSøk to listOfNotNull(
                                genererRequest(søk = søk, page = page, pageSize = pageSize, sort = sort, tags = emptyList()),
                            direkteMeldingerSøk)
                    }
                }
                if (søkOgRequest?.second != null) {
                    try {
                        val response = ledigeStillingerClient.finnLedigeStillinger(
                            call.securityContext().accessToken,
                            søkOgRequest.second
                        )
                        val jobbAnonnser = response.stillinger.map(::jobbAnnonse)
                        val svar = MineStillingerResponse(
                            soek = søkOgRequest.first.soek.api(),
                            resultat = jobbAnonnser,
                            sistKjoert = søkOgRequest.first.sistKjoet
                        )
                        val tidspunkt = clock.now()
                        transaction {
                            oppdaterSistKjøt(
                                søkOgRequest.first.id,
                                tidspunkt
                            )
                        }
                        tellLedigeStillingerKall(meterRegistry, tidspunkt, svar)
                        call.respond(svar)
                    } catch (e: Throwable) {
                        appLogger.error("Uventet feil: ", e)
                        throw e
                    }
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ProblemDetails(
                            title = "Ingen lagrede søk funnet",
                            detail = "Det ble ikke funnet noen lagrede søk for brukeren.",
                            instance = call.request.path(),
                            status = HttpStatusCode.NotFound
                        )
                    )
                }
            }
        }
    }
}

private fun jobbAnnonse(stilling: Stilling): ApiJobbAnnonse = ApiJobbAnnonse(
    tittel = stilling.tittel,
    stillingbeskrivelse = stilling.stillingstittel,
    publisert = stilling.publisert,
    soeknadsfrist = soeknadsfrist(stilling.soeknadsfrist),
    land = stilling.lokasjoner.map { it.land }.distinct().joinToString(", "),
    kommune = stilling.lokasjoner.mapNotNull { it.kommune }.distinct().joinToString(", ").takeIf(String::isNotBlank),
    sektor = when (stilling.sektor) {
        Sektor.OFFENTLIG -> no.naw.paw.minestillinger.api.Sektor.Offentlig
        Sektor.PRIVAT -> no.naw.paw.minestillinger.api.Sektor.Privat
        Sektor.UKJENT -> no.naw.paw.minestillinger.api.Sektor.Ukjent
    },
    selskap = stilling.arbeidsgivernavn ?: "Ukjent",
    arbeidsplassenNoId = stilling.uuid,
    tags = stilling.tags.map { it.toApiTag() }
)

fun soeknadsfrist(frist: Frist): Soeknadsfrist {
    val type = when (frist.type) {
        FristType.SNAREST -> SoeknadsfristType.Snarest
        FristType.FORTLOEPENDE -> SoeknadsfristType.Fortloepende
        FristType.DATO -> SoeknadsfristType.Dato
        FristType.UKJENT -> SoeknadsfristType.Ukjent
    }
    return Soeknadsfrist(
        raw = frist.verdi ?: "Ukjent",
        type = type,
        dato = frist.dato
    )
}

fun genererRequest(
    søk: StedSoek,
    page: Int,
    pageSize: Int,
    sort: ApiSortOrder,
    tags: List<Tag>
): FinnStillingerRequest = FinnStillingerByEgenskaperRequest(
    type = FinnStillingerType.BY_EGENSKAPER,
    soekeord = søk.soekeord,
    styrkkoder = søk.styrk08,
    fylker = søk.fylker.map { fylke ->
        Fylke(
            fylkesnummer = fylke.fylkesnummer,
            kommuner = fylke.kommuner.map { kommune ->
                Kommune(kommunenummer = kommune.kommunenummer)
            }
        )
    },
    paging = Paging(
        page = page, pageSize = pageSize, sortOrder = when (sort) {
            ApiSortOrder.ASC -> SortOrder.ASC
            ApiSortOrder.DESC -> SortOrder.DESC
        }
    ),
    tags = tags
)
