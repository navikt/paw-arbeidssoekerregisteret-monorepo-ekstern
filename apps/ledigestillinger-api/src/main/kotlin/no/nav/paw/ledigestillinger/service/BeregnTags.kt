package no.nav.paw.ledigestillinger.service

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
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
    }.also { tags ->
        Span.current()
            .addEvent("beregnet_tags",
                Attributes.of(
                    stringKey("source"), ad.source,
                    stringKey("direktemeldtStillingskategori"), ad.properties.find { prop ->
                        prop.key.equals("direktemeldtStillingskategori", ignoreCase = true)
                    }?.value ?: "null",
                    stringKey("tags"), tags.joinToString(",")
                )
            )
    }
}