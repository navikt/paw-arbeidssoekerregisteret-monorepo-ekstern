package no.nav.paw.ledigestillinger.test

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.ledigestillinger.config.APPLICATION_CONFIG
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.service.StillingService

data class TextContext(
    val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG),
    val meterRegistry: MeterRegistry = SimpleMeterRegistry(),
    val stillingService: StillingService = StillingService(applicationConfig, meterRegistry)
)