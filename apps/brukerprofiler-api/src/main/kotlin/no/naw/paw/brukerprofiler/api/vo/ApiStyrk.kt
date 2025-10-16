package no.naw.paw.brukerprofiler.api.vo

import no.naw.paw.brukerprofiler.kodeverk.SSBStyrkKode

data class ApiStyrk(
    val level: String,
    val code: String,
    val name: String
)

data class StyrkTreNode(
    val styrk: ApiStyrk,
    val children: List<StyrkTreNode>
) {
    fun prettyPrint(indent: String): String {
        val indent = "$indent.."
        return "$indent(level=${styrk.level}, code=${styrk.code}, name=${styrk.name}\n" +
                "${children.map { it.prettyPrint(indent) }.joinToString("") { it } }"
    }
}

private const val NO_PARENT = "no_parent"
fun List<SSBStyrkKode>.styrkTre(): List<StyrkTreNode> {
    val byParent: Map<String, List<SSBStyrkKode>> = this.groupBy { it.parentCode ?: NO_PARENT }
    val roots = byParent[NO_PARENT] ?: emptyList()
    return roots.map { root -> node(root, byParent) }
}

fun node(styrk: SSBStyrkKode, byParent: Map<String, List<SSBStyrkKode>>): StyrkTreNode {
    if (styrk.level == "4") {
        return StyrkTreNode(
            styrk = ApiStyrk(
                level = styrk.level,
                code = styrk.code,
                name = styrk.name
            ),
            children = emptyList()
        )
    } else {
        val children = byParent[styrk.code]?.map { child -> node(child, byParent) } ?: emptyList()
        return StyrkTreNode(
            styrk = ApiStyrk(
                level = styrk.level,
                code = styrk.code,
                name = styrk.name
            ),
            children = children
        )
    }
}