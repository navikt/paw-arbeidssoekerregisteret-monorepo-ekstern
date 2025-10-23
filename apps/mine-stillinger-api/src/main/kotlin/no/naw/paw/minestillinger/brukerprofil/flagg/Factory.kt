package no.naw.paw.minestillinger.brukerprofil.flagg

private val navnMap: Map<String, Flaggtype<*>> = Flaggtype::class
    .sealedSubclasses
    .mapNotNull { it.objectInstance }
    .associateBy { it.type }

fun flaggType(navn: String): Flaggtype<*>? = navnMap[navn]

fun ingenFlagg(): ListeMedFlagg = ListeMedFlagg(emptySet())

fun flaggListeOf(vararg flagg: Flagg): ListeMedFlagg = ListeMedFlagg(flagg.toSet())
