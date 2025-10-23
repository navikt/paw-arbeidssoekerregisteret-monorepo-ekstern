package no.naw.paw.minestillinger.domain

import java.time.Instant

private val navnMap: Map<String, FlaggNavn<*>> = FlaggNavn::class
    .sealedSubclasses
    .mapNotNull { it.objectInstance }
    .associateBy { it.navn }

fun flaggNavn(navn: String): FlaggNavn<*>? = navnMap[navn]

sealed interface FlaggNavn<A: FlaggVerdi> {
    val navn: String
    fun flagg(verdi: Boolean, tidspunkt: Instant): FlaggVerdi
}

object HarBruktTjenestenFlagg: FlaggNavn<HarBruktTjenesten> {
    override val navn: String = "har_brukt_tjenesten"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = HarBruktTjenesten(verdi, tidspunkt)
}

object OptOutFlagg: FlaggNavn<OptOut> {
    override val navn: String = "opt_out"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = OptOut(verdi, tidspunkt)
}

object HarGradertAdresseFlagg: FlaggNavn<HarGradertAdresse> {
    override val navn: String = "har_gradert_adresse"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = HarGradertAdresse(verdi, tidspunkt)
}

object TjenestenErAktivFlagg: FlaggNavn<TjenestenErAktiv> {
    override val navn: String = "tjenesten_er_aktiv"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = TjenestenErAktiv(verdi, tidspunkt)
}

object HarGodeMuligheterFlagg: FlaggNavn<HarGodeMuligheter> {
    override val navn: String = "har_gode_muligheter"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = HarGodeMuligheter(verdi, tidspunkt)
}

object ErITestGruppenFlagg: FlaggNavn<ErITestGruppen> {
    override val navn: String = "er_i_testgruppen"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = ErITestGruppen(verdi, tidspunkt)
}

fun ingenFlagg(): FlaggListe = FlaggListe(emptySet())


fun flaggListeOf(vararg flagg: FlaggVerdi): FlaggListe = FlaggListe(flagg.toSet())
class FlaggListe(flagg: Collection<FlaggVerdi>): Collection<FlaggVerdi> by flagg {
    private val map: Map<FlaggNavn<*>, FlaggVerdi> = flagg.associateBy { it.navn }
    init {
        require(flagg.size == flagg.map<FlaggVerdi, FlaggNavn<*>> { it.navn }.toSet().size) {
            "Duplikate flagg funnet i settet"
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <A: FlaggVerdi> get(navn: FlaggNavn<A>): A? = map[navn] as A?

    operator fun plus(other: FlaggVerdi): FlaggListe {
        return FlaggListe(map.values.plus(other).toSet())
    }

    operator fun minus(other: FlaggNavn<*>): FlaggListe {
        return FlaggListe(map.values.filter { it.navn != other }.toSet())
    }
    fun addOrUpdate(vararg others: FlaggVerdi): FlaggListe {
        val navn = others.map<FlaggVerdi, FlaggNavn<*>> { it.navn }.toSet()
        return FlaggListe(
            map.values
            .filter { it.navn !in navn }
            .plus(others)
            .toSet()
        )
    }
    fun addOrIgnore(vararg others: FlaggVerdi): FlaggListe {
        val add = others.filter { other ->
            map.containsKey(other.navn).not()
        }
        return FlaggListe(map.values + add)
    }
}

sealed interface FlaggVerdi {
    val navn: FlaggNavn<*>
    val verdi: Boolean
    val tidspunkt: Instant
}

data class HarGodeMuligheter(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): FlaggVerdi {
    override val navn = HarGodeMuligheterFlagg
}

data class ErITestGruppen(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): FlaggVerdi {
    override val navn = ErITestGruppenFlagg
}

data class HarBruktTjenesten(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): FlaggVerdi {
    override val navn = HarBruktTjenestenFlagg
}

data class OptOut(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): FlaggVerdi {
    override val navn = OptOutFlagg
}

data class HarGradertAdresse(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): FlaggVerdi {
    override val navn = HarGradertAdresseFlagg
}

data class TjenestenErAktiv(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): FlaggVerdi {
    override val navn = TjenestenErAktivFlagg
}