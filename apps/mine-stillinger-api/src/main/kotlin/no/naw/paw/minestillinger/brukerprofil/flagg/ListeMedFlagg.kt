package no.naw.paw.minestillinger.brukerprofil.flagg

class ListeMedFlagg(flagg: Collection<Flagg>): Collection<Flagg> by flagg {
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

    operator fun plus(other: Flagg): ListeMedFlagg {
        return ListeMedFlagg(map.values.plus(other).toSet())
    }

    operator fun minus(other: Flaggtype<*>): ListeMedFlagg {
        return ListeMedFlagg(map.values.filter { it.type != other }.toSet())
    }

    fun addOrUpdate(vararg others: Flagg): ListeMedFlagg {
        val navn = others.map<Flagg, Flaggtype<*>> { it.type }.toSet()
        return ListeMedFlagg(
            map.values
            .filter { it.type !in navn }
            .plus(others)
            .toSet()
        )
    }
    fun addOrIgnore(vararg others: Flagg): ListeMedFlagg {
        val add = others.filter { other ->
            map.containsKey(other.type).not()
        }
        return ListeMedFlagg(map.values + add)
    }
}