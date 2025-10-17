package no.naw.paw.minestillinger.db

import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Table
import org.postgresql.util.PGobject

fun <A: Table> A.jsonb(name: String) = this.registerColumn(name, JsonbColumnType(false))

class JsonbColumnType(override var nullable: Boolean): IColumnType<String> {

    override fun sqlType(): String = "JSONB"

    override fun valueFromDB(value: Any): String? {
        if (value is PGobject) {
            if (value.type.equals(sqlType(), true)) {
                return value.value
            } else {
                throw IllegalArgumentException("Value is not a JSONB object: ${value.type}")
            }
        } else {
            throw IllegalArgumentException("Value is not a PGobject: ${value.javaClass}")
        }
    }

    override fun valueToDB(value: String?): Any {
        return PGobject().apply {
            type = sqlType()
            this.value = value
        }
    }
}