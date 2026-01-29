package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.common.types.identer.AktorId

fun no.nav.paw.felles.model.AktorId.asCommonAktorId(): AktorId = AktorId(this.value)