package no.nav.paw.arbeidssoekerregisteret.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val buildApplicationLogger: Logger get() = LoggerFactory.getLogger("no.nav.paw.logger.application")