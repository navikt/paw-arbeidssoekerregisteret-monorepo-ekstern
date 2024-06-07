package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import java.time.Duration
import java.time.Instant

class PeriodeTopologyTest : FreeSpec({

    with(PeriodeTopologyTestContext()) {
        "Testsuite for toggling av microfrontends basert på arbeidssøkerperiode" - {
            val identitetsnummer = "01017012345"
            val arbeidsoekerId = 1234L
            val avsluttetGammeltTidspunkt = Instant.now().minus(Duration.ofDays(27))
            val startetGammeltTidspunkt = avsluttetGammeltTidspunkt.minus(Duration.ofDays(10))
            val avsluttetTidspunkt = Instant.now()
            val startetTidspunkt = avsluttetTidspunkt.minus(Duration.ofDays(10))
            val startetPeriode = buildPeriode(identitetsnummer = identitetsnummer, startet = startetTidspunkt)
            val avsluttetPeriode = buildPeriode(
                identitetsnummer = identitetsnummer,
                startet = startetTidspunkt,
                avsluttet = avsluttetTidspunkt
            )
            val avsluttetGammelPeriode = buildPeriode(
                identitetsnummer = identitetsnummer,
                startet = startetGammeltTidspunkt,
                avsluttet = avsluttetGammeltTidspunkt
            )
            val key = 9876L
            every { kafkaKeysClientMock.hentKafkaKeys(identitetsnummer) } returns KafkaKeysResponse(
                arbeidsoekerId,
                key
            )

            "Skal aktivere nødvendige microfrontends ved start av periode" {
                periodeTopic.pipeInput(key, startetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe arbeidsoekerId
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe arbeidsoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                periodeKeyValueStore.size() shouldBe 1
                with(periodeKeyValueStore.get(arbeidsoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe startetPeriode.id
                    identitetsnummer shouldBe startetPeriode.identitetsnummer
                    arbeidssoekerId shouldBe arbeidsoekerId
                    startet shouldBe startetPeriode.startet.tidspunkt
                    avsluttet shouldBe null
                }
            }

            "Skal deaktivere aia-behovsvurdering microfrontend ved avslutting av periode" {
                periodeTopic.pipeInput(key, avsluttetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe arbeidsoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe avsluttetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                periodeKeyValueStore.size() shouldBe 1
                with(periodeKeyValueStore.get(arbeidsoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe avsluttetPeriode.id
                    identitetsnummer shouldBe avsluttetPeriode.identitetsnummer
                    arbeidssoekerId shouldBe arbeidsoekerId
                    startet shouldBe avsluttetPeriode.startet.tidspunkt
                    avsluttet shouldBe avsluttetPeriode.avsluttet.tidspunkt
                }
            }

            "Skal deaktivere aia-min-side microfrontend 21 dager etter avslutting av periode" {
                testDriver.advanceWallClockTime(Duration.ofDays(22))

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe arbeidsoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe avsluttetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                periodeKeyValueStore.size() shouldBe 0
            }

            "Skal filtrere vekk perioder som er avsluttet og eldre enn 26 dager" {
                periodeTopic.pipeInput(key, avsluttetGammelPeriode)

                microfrontendTopic.isEmpty shouldBe true
                periodeKeyValueStore.size() shouldBe 0
            }
        }
    }
})