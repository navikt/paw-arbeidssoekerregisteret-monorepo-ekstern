package no.nav.paw.ledigestillinger.service

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder

fun Column<List<String>>.overlapWith(other: List<String>): Op<Boolean> =
    object : Op<Boolean>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
            queryBuilder {
                append(this@overlapWith)
                append(" && ARRAY[")
                other.forEachIndexed { index, value ->
                    if (index > 0) append(", ")
                    append("'$value'")
                }
                append("]::varchar[]")
            }
        }
    }