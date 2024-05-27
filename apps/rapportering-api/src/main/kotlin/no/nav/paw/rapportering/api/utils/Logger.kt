package no.nav.paw.rapportering.api.utils

import org.slf4j.LoggerFactory

inline val <reified T> T.logger
    get() = LoggerFactory.getLogger(T::class.java)