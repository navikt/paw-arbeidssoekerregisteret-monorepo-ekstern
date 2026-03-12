package no.naw.paw.minestillinger.api.vo

import no.naw.paw.ledigestillinger.model.Tag

enum class ApiTag {
    DIREKTEMELDT_V1
}

fun Tag.toApiTag(): ApiTag {
    return when (this) {
        Tag.DIREKTEMELDT_V1 -> ApiTag.DIREKTEMELDT_V1
    }}