package no.naw.paw.minestillinger

fun runApp(applicationContext: ApplicationContext): Unit {
    Thread.currentThread().uncaughtExceptionHandler =
        Thread.UncaughtExceptionHandler { _, e ->
            appLogger.error("Uventet feil i applikasjonen", e)
            System.exit(1)
        }
    val ktorInstance = initEmbeddedKtorServer(
        prometheusRegistry = applicationContext.prometheusMeterRegistry,
        meterBinders = applicationContext.meterBinders,
        healthIndicator = applicationContext.healthChecks,
        authProviders = applicationContext.securityConfig.authProviders,
        brukerprofilTjeneste = applicationContext.brukerprofilTjeneste,
        finnStillingerClient = applicationContext.finnStillingerClient,
        clock = applicationContext.clock
    )
    Runtime.getRuntime().addShutdownHook(Thread {
        appLogger.info("Applikasjonen avsluttes...")
        runCatching {
            appLogger.info("Avslutter Ktor...")
            ktorInstance.stop(1000, 1500)
            appLogger.info("Ktor avsluttet.")
        }.onFailure { cause ->
            appLogger.info("Feil av ved avslutting av Ktor", cause)
        }
        applicationContext.consumer.close()
    })
    applicationContext.dataSource.use {
        applicationContext.consumer.runAndCloseOnExit()
            .handle { _, error ->
                if (error != null) {
                    appLogger.error("Kafka consumer stoppet grunnet feil", error)
                } else {
                    appLogger.info("Kafka consumer stoppet")
                }
            }
        ktorInstance.start(wait = true)
    }
    appLogger.info("Applikasjonen er stoppet.")
}