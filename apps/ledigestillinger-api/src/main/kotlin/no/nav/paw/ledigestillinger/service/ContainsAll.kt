package no.nav.paw.ledigestillinger.service

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.and

private val varcharType = VarCharColumnType()

fun Column<List<String>>.containsAll(other: List<String>): Op<Boolean> {
    if (other.isEmpty()) {
        return Op.TRUE
    }
    return other
        .map { value -> anyElement(this, value) }
        .reduce { acc, op -> acc and op }
}

private fun anyElement(column: Column<List<String>>, value: String): Op<Boolean> =
    object : Op<Boolean>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
            queryBuilder {
                registerArgument(varcharType, value)
                append(" = ANY(")
                append(column)
                append(")")
            }
        }
    }