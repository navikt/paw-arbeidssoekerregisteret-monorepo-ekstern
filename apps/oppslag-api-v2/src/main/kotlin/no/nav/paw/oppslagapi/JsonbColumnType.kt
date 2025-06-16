package no.nav.paw.oppslagapi

import no.nav.paw.oppslagapi.DataTable.registerColumn
import org.jetbrains.exposed.sql.IColumnType
import org.postgresql.util.PGobject
import kotlin.apply
import kotlin.jvm.javaClass
import kotlin.text.equals

fun jsonb(name: String) = registerColumn(name, JsonbColumnType(false))

class JsonbColumnType(override var nullable: Boolean): IColumnType<String> {

    override fun sqlType(): String = "JSONB"

    override fun valueFromDB(value: Any): String? {
        if (value is PGobject) {
            if (value.type.equals(sqlType(), true)) {
                return value.value
            } else {
                throw kotlin.IllegalArgumentException("Value is not a JSONB object: ${value.type}")
            }
        } else {
            throw kotlin.IllegalArgumentException("Value is not a PGobject: ${value.javaClass}")
        }
    }

    override fun valueToDB(value: String?): Any {
        return PGobject().apply {
            type = sqlType()
            this.value = value
        }
    }
}