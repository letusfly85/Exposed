package org.jetbrains.exposed.sql.vendors

import org.jetbrains.exposed.exceptions.throwUnsupportedException
import org.jetbrains.exposed.sql.*

internal object SQLiteDataTypeProvider : DataTypeProvider() {
    override fun shortAutoincType(): String = "INTEGER AUTO_INCREMENT"
    override fun longAutoincType(): String = "INTEGER AUTO_INCREMENT"
    override fun floatType(): String = "SINGLE"
    override fun booleanToStatementString(bool: Boolean) = if (bool) "1" else "0"
    override fun dateTimeType(): String  = "NUMERIC"
    override val blobAsStream: Boolean = true
}

internal object SQLiteFunctionProvider : FunctionProvider() {
    override fun<T:String?> substring(expr: Expression<T>, start: Expression<Int>, length: Expression<Int>, builder: QueryBuilder): String =
            super.substring(expr, start, length, builder).replace("SUBSTRING", "substr")

    override fun insert(ignore: Boolean, table: Table, columns: List<Column<*>>, expr: String, transaction: Transaction): String {
        val def = super.insert(false, table, columns, expr, transaction)
        return if (ignore) def.replaceFirst("INSERT", "INSERT OR IGNORE") else def
    }

    override fun delete(ignore: Boolean, table: Table, where: String?, transaction: Transaction): String {
        val def = super.delete(false, table, where, transaction)
        return if (ignore) def.replaceFirst("DELETE", "DELETE OR IGNORE") else def
    }

    override fun update(targets: ColumnSet, columnsAndValues: List<Pair<Column<*>, Any?>>, limit: Int?, where: Op<Boolean>?, transaction: Transaction): String {
        if (limit != null) transaction.throwUnsupportedException("SQLite doesn't support LIMIT in UPDATE clause.")
        return super.update(targets, columnsAndValues, limit, where, transaction)
    }
}

internal class SQLiteDialect : VendorDialect(dialectName, SQLiteDataTypeProvider, SQLiteFunctionProvider) {
    override val supportsMultipleGeneratedKeys: Boolean = false
    override fun isAllowedAsColumnDefault(e: Expression<*>): Boolean = true

    override fun getDatabase(): String = ""

    override fun createIndex(index: Index): String {
        val originalCreateIndex = super.createIndex(index.copy(unique = false))
        return if (index.unique) originalCreateIndex.replace("INDEX", "UNIQUE INDEX")
        else originalCreateIndex
    }

    companion object {
        const val dialectName = "sqlite"
    }
}