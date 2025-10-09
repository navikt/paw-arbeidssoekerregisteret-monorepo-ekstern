package no.naw.paw.brukerprofiler

fun runApp(applicationContext: ApplicationContext): Unit {
    Thread.currentThread().uncaughtExceptionHandler =
        Thread.UncaughtExceptionHandler { _, e ->
            appLogger.error("Uventet feil i applikasjonen", e)
            System.exit(1)
        }
    val ktorInstance = initEmbeddedKtorServer(
        prometheusRegistry = applicationContext.prometheusMeterRegistry,
        meterBinders = listOf(),
        healthIndicator = applicationContext.healthChecks,
        authProviders = applicationContext.securityConfig.authProviders,
    )
    Runtime.getRuntime().addShutdownHook(Thread {
        appLogger.info("Applikasjonen avsluttes...")
        runCatching { ktorInstance.stop(1000, 1500) }
        applicationContext.consumer.close()
    })
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
        ktorInstance.start(wait = true)
    }
    appLogger.info("Applikasjonen er stoppet.")
}