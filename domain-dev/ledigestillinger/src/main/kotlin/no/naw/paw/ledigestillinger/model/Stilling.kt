package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class Stilling(

    @get:JsonProperty("uuid")
    val uuid: UUID,

    @get:JsonProperty("tittel")
    val tittel: String,

    @get:JsonProperty("status")
    val status: StillingStatus,

    @get:JsonProperty("sektor")
    val sektor: Sektor,

    @get:JsonProperty("kategorier")
    val kategorier: List<Kategori>,

    @get:JsonProperty("lokasjoner")
    val lokasjoner: List<Lokasjon>,

    @get:JsonProperty("soeknadsfrist")
    val soeknadsfrist: Soeknadsfrist,

    @get:JsonProperty("publisert")
    val publisert: Instant,

    @get:JsonProperty("adnr")
    val adnr: String? = null,

    @get:JsonProperty("jobbtittel")
    val jobbtittel: String? = null,

    @get:JsonProperty("ansettelsesform")
    val ansettelsesform: String? = null,

    @get:JsonProperty("ansettelsesprosent")
    val ansettelsesprosent: String? = null,

    @get:JsonProperty("stillingsantall")
    val stillingsantall: String? = null,

    @get:JsonProperty("arbeidsgivernavn")
    val arbeidsgivernavn: String? = null,

    @get:JsonProperty("arbeidsgiver")
    val arbeidsgiver: Arbeidsgiver? = null,

    @get:JsonProperty("utloeper")
    val utloeper: Instant? = null
)