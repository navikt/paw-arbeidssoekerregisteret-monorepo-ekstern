package no.naw.paw.minestillinger.brukerprofil.flagg

import no.naw.paw.minestillinger.brukerprofil.flagg.ListeMedFlagg

class ListeMedFlagg private constructor(
    flagg: Collection<Flagg>,
    private val dirty: Set<LagretFlagg>
): Collection<Flagg> by flagg {
    companion object {
        fun listeMedFlagg(iterable: Iterable<Flagg>): ListeMedFlagg = ListeMedFlagg(
            flagg = iterable.toSet(),
            dirty = emptySet()
        )
    }

    private val map: Map<Flaggtype<*>, Flagg> = flagg.associateBy { it.type }

    init {
        require(flagg.size == flagg.map<Flagg, Flaggtype<*>> { it.type }.toSet().size) {
            "Duplikate flagg funnet i settet"
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <A: Flagg> get(navn: Flaggtype<A>): A? = map[navn] as A?

    fun <A: Flagg> isTrue(navn: Flaggtype<A>): Boolean = this[navn]?.verdi == true

    fun <A: Flagg> isFalse(navn: Flaggtype<A>): Boolean = !isTrue(navn)

    val flaggSomMåOppdateres: Set<LagretFlagg>
        get() = dirty

    fun clean(): ListeMedFlagg {
        return ListeMedFlagg(
            flagg = map.values,
            dirty = setOf()
        )
    }

    operator fun plus(other: Flagg): ListeMedFlagg {
        return ListeMedFlagg(
            flagg = map.values.plus(other).toSet(),
            dirty = if (other is LagretFlagg) dirty.plus(other) else dirty
        )
    }

    fun replace(other: Flagg): ListeMedFlagg {
        return ListeMedFlagg(
            flagg = map.values
            .filter { it.type != other.type }
            .plus(other)
            .toSet(),
            dirty = if (other is LagretFlagg) dirty.plus(other) else dirty
        )
    }

    fun addOrUpdate(vararg others: Flagg): ListeMedFlagg {
        val endret = others.filter { other ->
            map[other.type]?.verdi != other.verdi
        }
        val endretType = endret.map { it.type }
        return ListeMedFlagg(
            flagg = map.values
            .filter { it.type !in endretType }
            .plus(endret)
            .toSet(),
            dirty = dirty.plus(endret.filterIsInstance<LagretFlagg>())
        )
    }
}
