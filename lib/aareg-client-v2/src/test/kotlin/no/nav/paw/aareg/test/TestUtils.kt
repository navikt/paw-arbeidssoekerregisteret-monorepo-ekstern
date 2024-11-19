package no.nav.paw.aareg.test

internal fun String.readResource(): String =
    ClassLoader.getSystemResource(this).readText()
