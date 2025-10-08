package no.naw.paw.brukerprofiler

fun runApp(applicationContext: ApplicationContext): Unit {
    applicationContext.dataSource.use {
// Deaktiverer meldingse-konsumering til logikken er klar
//      applicationContext.consumer.runAndCloseOnExit()
//            .handle { _, error ->
//                if (error != null) {
//                    appLogger.error("Kafka consumer stoppet grunnet feil", error)
//                } else {
//                    appLogger.info("Kafka consumer stoppet")
//                }
//            }
        initEmbeddedKtorServer(
            prometheusRegistry = applicationContext.prometheusMeterRegistry,
            meterBinders = listOf(),
            healthIndicator = applicationContext.healthChecks,
            authProviders = applicationContext.securityConfig.authProviders,
        ).start(wait = true)
    }
}