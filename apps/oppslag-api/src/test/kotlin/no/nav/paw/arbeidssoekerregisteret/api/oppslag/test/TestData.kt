package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.AvviksTypeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BeskrivelseMedDetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerType
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.JobbSituasjonBeskrivelse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.NavAnsatt
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringsResultat
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeResponse
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

object TestData {
    val identitetsnummer = Identitetsnummer("18908396568")
    val navAnsatt = NavAnsatt(UUID.randomUUID().toString(), "Z999999")
    val periodeId = UUID.randomUUID()
}

typealias Arbeidssoekerperioder = List<ArbeidssoekerperiodeResponse>

fun getArbeidssoekerperiodeResponse(sistePeriodeId: UUID): Arbeidssoekerperioder {
    return listOf(
        ArbeidssoekerperiodeResponse(
            periodeId = UUID.randomUUID(),
            startet =
            MetadataResponse(
                tidspunkt = Instant.now().minus(2, ChronoUnit.DAYS),
                utfoertAv =
                BrukerResponse(
                    type = BrukerType.SLUTTBRUKER,
                    id = "12345678901"
                ),
                kilde = "arbeidssokerregisteret",
                aarsak = "",
                tidspunktFraKilde =
                TidspunktFraKildeResponse(
                    tidspunkt = Instant.now().minus(2, ChronoUnit.DAYS),
                    avviksType = AvviksTypeResponse.RETTING
                )
            )
        ),
        ArbeidssoekerperiodeResponse(
            periodeId = UUID.randomUUID(),
            startet =
            MetadataResponse(
                tidspunkt = Instant.now().minus(1, ChronoUnit.DAYS),
                utfoertAv =
                BrukerResponse(
                    type = BrukerType.SLUTTBRUKER,
                    id = "12345678901"
                ),
                kilde = "arbeidssokerregisteret",
                aarsak = "",
                tidspunktFraKilde =
                TidspunktFraKildeResponse(
                    tidspunkt = Instant.now().minus(1, ChronoUnit.DAYS),
                    avviksType = AvviksTypeResponse.RETTING
                )
            )
        ),
        ArbeidssoekerperiodeResponse(
            periodeId = sistePeriodeId,
            startet =
            MetadataResponse(
                tidspunkt = Instant.now(),
                utfoertAv =
                BrukerResponse(
                    type = BrukerType.SLUTTBRUKER,
                    id = "12345678901"
                ),
                kilde = "arbeidssokerregisteret",
                aarsak = "",
                tidspunktFraKilde =
                TidspunktFraKildeResponse(
                    tidspunkt = Instant.now(),
                    avviksType = AvviksTypeResponse.RETTING
                )
            )
        )
    )
}

fun getOpplysningerOmArbeidssoekerResponse(periodeId: UUID): List<OpplysningerOmArbeidssoekerResponse> =
    listOf(
        OpplysningerOmArbeidssoekerResponse(
            opplysningerOmArbeidssoekerId = UUID.randomUUID(),
            periodeId = periodeId,
            sendtInnAv =
            MetadataResponse(
                tidspunkt = Instant.now(),
                utfoertAv =
                BrukerResponse(
                    type = BrukerType.SLUTTBRUKER,
                    id = "12345678901"
                ),
                kilde = "arbeidssokerregisteret",
                aarsak = "",
                tidspunktFraKilde = null
            ),
            jobbsituasjon =
            listOf(
                BeskrivelseMedDetaljerResponse(
                    beskrivelse = JobbSituasjonBeskrivelse.ANNET,
                    detaljer = mapOf("test" to "test")
                )
            )
        ),
        OpplysningerOmArbeidssoekerResponse(
            opplysningerOmArbeidssoekerId = UUID.randomUUID(),
            periodeId = UUID.randomUUID(),
            sendtInnAv =
            MetadataResponse(
                tidspunkt = Instant.now().minus(1, ChronoUnit.DAYS),
                utfoertAv =
                BrukerResponse(
                    type = BrukerType.SLUTTBRUKER,
                    id = "12345678901"
                ),
                kilde = "arbeidssokerregisteret",
                aarsak = "",
                tidspunktFraKilde = null
            ),
            jobbsituasjon =
            listOf(
                BeskrivelseMedDetaljerResponse(
                    beskrivelse = JobbSituasjonBeskrivelse.ANNET,
                    detaljer = mapOf("test" to "test")
                )
            )
        ),
        OpplysningerOmArbeidssoekerResponse(
            opplysningerOmArbeidssoekerId = UUID.randomUUID(),
            periodeId = UUID.randomUUID(),
            sendtInnAv =
            MetadataResponse(
                tidspunkt = Instant.now().minus(2, ChronoUnit.DAYS),
                utfoertAv =
                BrukerResponse(
                    type = BrukerType.SLUTTBRUKER,
                    id = "12345678901"
                ),
                kilde = "arbeidssokerregisteret",
                aarsak = "",
                tidspunktFraKilde = null
            ),
            jobbsituasjon =
            listOf(
                BeskrivelseMedDetaljerResponse(
                    beskrivelse = JobbSituasjonBeskrivelse.ANNET,
                    detaljer = mapOf("test" to "test")
                )
            )
        )
    )

fun getProfileringResponse(periodeId: UUID): List<ProfileringResponse> =
    listOf(
        ProfileringResponse(
            profileringId = UUID.randomUUID(),
            periodeId = periodeId,
            opplysningerOmArbeidssoekerId = UUID.randomUUID(),
            sendtInnAv =
            MetadataResponse(
                tidspunkt = Instant.now(),
                utfoertAv =
                BrukerResponse(
                    type = BrukerType.SLUTTBRUKER,
                    id = "12345678901"
                ),
                kilde = "arbeidssokerregisteret",
                aarsak = "",
                tidspunktFraKilde = null
            ),
            profilertTil = ProfileringsResultat.ANTATT_GODE_MULIGHETER,
            null,
            null
        ),
        ProfileringResponse(
            profileringId = UUID.randomUUID(),
            periodeId = UUID.randomUUID(),
            opplysningerOmArbeidssoekerId = UUID.randomUUID(),
            sendtInnAv =
            MetadataResponse(
                tidspunkt = Instant.now().minus(1, ChronoUnit.DAYS),
                utfoertAv =
                BrukerResponse(
                    type = BrukerType.SLUTTBRUKER,
                    id = "12345678901"
                ),
                kilde = "arbeidssokerregisteret",
                aarsak = "",
                tidspunktFraKilde = null
            ),
            profilertTil = ProfileringsResultat.ANTATT_GODE_MULIGHETER,
            null,
            null
        ),
        ProfileringResponse(
            profileringId = UUID.randomUUID(),
            periodeId = UUID.randomUUID(),
            opplysningerOmArbeidssoekerId = UUID.randomUUID(),
            sendtInnAv =
            MetadataResponse(
                tidspunkt = Instant.now().minus(2, ChronoUnit.DAYS),
                utfoertAv =
                BrukerResponse(
                    type = BrukerType.SLUTTBRUKER,
                    id = "12345678901"
                ),
                kilde = "arbeidssokerregisteret",
                aarsak = "",
                tidspunktFraKilde = null
            ),
            profilertTil = ProfileringsResultat.ANTATT_GODE_MULIGHETER,
            null,
            null
        )
    )
