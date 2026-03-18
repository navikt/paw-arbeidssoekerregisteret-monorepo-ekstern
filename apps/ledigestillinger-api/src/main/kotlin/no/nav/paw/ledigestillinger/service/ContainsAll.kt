package no.nav.paw.ledigestillinger.service

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.VarCharColumnType

private val varcharType = VarCharColumnType()

fun Column<List<String>>.containsAll(other: List<String>): Op<Boolean> =
    object : Op<Boolean>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
            queryBuilder {
                append(this@containsAll)
                append(" @> ARRAY[")
                other.forEachIndexed { index, value ->
                    if (index > 0) {
                        append(",")
                    }
                    registerArgument(varcharType, value)
                }
                append("]::varchar[]")
            }
        }
    }