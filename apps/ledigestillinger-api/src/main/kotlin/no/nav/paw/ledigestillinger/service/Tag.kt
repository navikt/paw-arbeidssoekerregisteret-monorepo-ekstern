package no.nav.paw.ledigestillinger.service

enum class Tag {
    DIREKTEMELDT_V1
}

fun asDto(tag: Tag):  no.naw.paw.ledigestillinger.model.Tag {
    return when (tag) {
        Tag.DIREKTEMELDT_V1 -> no.naw.paw.ledigestillinger.model.Tag.DIREKTEMELDT_STILLING_V1
    }
}

fun fromDto(dto: no.naw.paw.ledigestillinger.model.Tag): Tag {
    return when (dto) {
        no.naw.paw.ledigestillinger.model.Tag.DIREKTEMELDT_STILLING_V1 -> Tag.DIREKTEMELDT_V1
    }
}

fun Iterable<Tag>.asDto(): List<no.naw.paw.ledigestillinger.model.Tag> {
    return this.map(::asDto)
}

fun Iterable<no.naw.paw.ledigestillinger.model.Tag>.fromDto(): List<Tag> {
    return this.map(::fromDto)
}