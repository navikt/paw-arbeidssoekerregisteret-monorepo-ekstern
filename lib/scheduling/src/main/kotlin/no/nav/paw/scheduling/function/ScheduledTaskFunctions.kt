package no.nav.paw.scheduling.function

import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.scheduling")

fun defaultSuccessFunction() {
    logger.debug("Scheduled task run succeeded")
}

fun defaultErrorFunction(throwable: Throwable) {
    logger.error("Scheduled task run failed", throwable)
    throw throwable
}
