package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class Profilering(

    @get:JsonProperty("profileringId")
    val profileringId: UUID,

    @get:JsonProperty("profilertTil")
    val profilertTil: ProfilertTil
)