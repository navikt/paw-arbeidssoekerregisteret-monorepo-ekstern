package no.nav.paw.ledigestillinger.service

import no.nav.pam.stilling.ext.avro.Ad

fun beregnTags(ad: Ad): Set<Tag> {
    val sourceErDir = ad.source.equals("DIR", ignoreCase = true)
    val dirMeldtKatErStilling = ad.properties.find {
        it.key.equals("direktemeldtStillingskategori", ignoreCase = true)
    }?.value.equals("STILLING", ignoreCase = true)
    return if (sourceErDir && dirMeldtKatErStilling) {
        hashSetOf(Tag.DIREKTEMELDT_V1)
    } else {
        emptySet()
    }
}