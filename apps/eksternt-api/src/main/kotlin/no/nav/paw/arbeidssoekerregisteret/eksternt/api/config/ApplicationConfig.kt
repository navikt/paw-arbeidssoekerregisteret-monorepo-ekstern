package no.nav.paw.arbeidssoekerregisteret.eksternt.api.config

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.TimeUtils
import java.time.Duration

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val gruppeId: String,
    val periodeTopic: String,
    val perioderVedlikeholdTaskDelay: Duration = TimeUtils.tidTilMidnatt(),
    val perioderVedlikeholdTaskInterval: Duration,
    val perioderMetricsTaskDelay: Duration = Duration.ZERO,
    val perioderMetricsTaskInterval: Duration
)
