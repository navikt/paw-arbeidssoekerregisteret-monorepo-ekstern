package no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T : Any> T.buildLogger get(): Logger = LoggerFactory.getLogger(T::class.java.name)

inline val buildApplicationLogger: Logger get() = LoggerFactory.getLogger("no.nav.paw.logger.application")
inline val buildErrorLogger: Logger get() = LoggerFactory.getLogger("no.nav.paw.logger.error")
