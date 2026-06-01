package no.naw.paw.minestillinger.domain

import no.naw.paw.ledigestillinger.model.Tag
import no.naw.paw.minestillinger.api.vo.ApiSoekeTag
import no.naw.paw.minestillinger.domain.SoekeTag.INGEN_KRAV_TIL_ARBEIDSERFARING_V1
import no.naw.paw.minestillinger.domain.SoekeTag.INGEN_KRAV_TIL_FOERERKORT_V1
import no.naw.paw.minestillinger.domain.SoekeTag.INGEN_KRAV_TIL_UTDANNING_V1

enum class SoekeTag {
    INGEN_KRAV_TIL_ARBEIDSERFARING_V1,
    INGEN_KRAV_TIL_UTDANNING_V1,
    INGEN_KRAV_TIL_FOERERKORT_V1;

    fun toTag(): Tag {
        return when (this) {
            INGEN_KRAV_TIL_ARBEIDSERFARING_V1 -> Tag.INGEN_KRAV_TIL_ARBEIDSERFARING_V1
            INGEN_KRAV_TIL_UTDANNING_V1 -> Tag.INGEN_KRAV_TIL_UTDANNING_V1
            INGEN_KRAV_TIL_FOERERKORT_V1 -> Tag.INGEN_KRAV_TIL_FOERERKORT_V1
        }
    }
}

fun soekeTag(soeke: ApiSoekeTag): SoekeTag {
    return when (soeke) {
        ApiSoekeTag.INGEN_KRAV_TIL_ARBEIDSERFARING_V1 -> INGEN_KRAV_TIL_ARBEIDSERFARING_V1
        ApiSoekeTag.INGEN_KRAV_TIL_UTDANNING_V1 -> INGEN_KRAV_TIL_UTDANNING_V1
        ApiSoekeTag.INGEN_KRAV_TIL_FOERERKORT_V1 -> INGEN_KRAV_TIL_FOERERKORT_V1
    }
}

fun apiSpoekeTag(soeke: SoekeTag): ApiSoekeTag {
    return when (soeke) {
        INGEN_KRAV_TIL_ARBEIDSERFARING_V1 -> ApiSoekeTag.INGEN_KRAV_TIL_ARBEIDSERFARING_V1
        INGEN_KRAV_TIL_UTDANNING_V1 -> ApiSoekeTag.INGEN_KRAV_TIL_UTDANNING_V1
        INGEN_KRAV_TIL_FOERERKORT_V1 -> ApiSoekeTag.INGEN_KRAV_TIL_FOERERKORT_V1
    }
}

