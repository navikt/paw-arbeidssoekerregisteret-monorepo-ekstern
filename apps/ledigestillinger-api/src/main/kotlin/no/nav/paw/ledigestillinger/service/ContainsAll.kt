package no.nav.paw.ledigestillinger.service

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder

fun Column<List<String>>.containsAll(other: List<String>): Op<Boolean> =
    object : Op<Boolean>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
            queryBuilder {
                append(this@containsAll)
                append(" @> ")
                registerArgument(this@containsAll.columnType, other)
                append("::varchar[]")
            }
        }
    }