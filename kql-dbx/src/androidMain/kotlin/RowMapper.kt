import android.database.Cursor
import android.database.sqlite.SQLiteStatement

import java.math.BigDecimal

/**
 * The `Row` class provides a way to retrieve data from a single row of a [Cursor].
 * It supports retrieving values based on the column index or name, by its type.
 *
 * @throws [IllegalStateException] if the type is not supported.
 */
@JvmInline
value class Row(val cursor: Cursor) {
    /**
     * Retrieves a nullable value of type [T] from the current row of the [Cursor] based on the type of the column.
     * @param arg The name of the column to retrieve the value from.
     */
    inline fun <reified T : Any?> get(arg: String): T? {
        val columnIndex = cursor.getColumnIndexOrThrow(arg)
        if (cursor.isNull(columnIndex)) return null
        return when (T::class) {
            String::class -> cursor.getString(columnIndex) as T
            Int::class -> cursor.getInt(columnIndex) as T
            Long::class -> cursor.getLong(columnIndex) as T
            Float::class -> cursor.getFloat(columnIndex) as T
            Double::class -> cursor.getDouble(columnIndex) as T
            BigDecimal::class -> BigDecimal(cursor.getString(columnIndex)) as T
            Short::class -> cursor.getShort(columnIndex) as T
            Boolean::class -> (cursor.getInt(columnIndex) == 1) as T
            ByteArray::class -> cursor.getBlob(columnIndex) as T
            Byte::class -> cursor.getShort(columnIndex).toByte() as T
            else -> error("Unsupported type: ${T::class}")
        }
    }

    /**
     * Retrieves a value of type [T] from the current row of the [Cursor] based on the index of the column.
     * @param arg The index of the column to retrieve the value from.
     */
    inline operator fun <reified T : Any> get(arg: Int): T? {
        if (cursor.isNull(arg)) return null
        return when (T::class) {
            String::class -> cursor.getString(arg) as T
            Int::class -> cursor.getInt(arg) as T
            Long::class -> cursor.getLong(arg) as T
            Float::class -> cursor.getFloat(arg) as T
            Double::class -> cursor.getDouble(arg) as T
            BigDecimal::class -> BigDecimal(cursor.getString(arg)) as T
            Short::class -> cursor.getShort(arg) as T
            Boolean::class -> (cursor.getInt(arg) == 1) as T
            ByteArray::class -> cursor.getBlob(arg) as T
            Byte::class -> cursor.getShort(arg).toByte() as T
            else -> error("Unsupported type: ${T::class}")
        }
    }
}

/**
 * The `Rows` class provides an iterable interface for iterating through multiple rows in a [Cursor].
 * Each iteration returns a `Row` object.
 */
@JvmInline
value class Rows(private val cursor: Cursor) : Iterable<Row>, AutoCloseable {
    override fun iterator(): Iterator<Row> = cursor.iterator()

    override fun close() = cursor.close()
}

/** Converts a [Cursor] to an [Iterator] of [Row] objects. */
fun Cursor.iterator(): Iterator<Row> =
    object : Iterator<Row> {
        var itHasNext: Boolean? = null

        override fun hasNext() = itHasNext ?: this@iterator.moveToNext().also { itHasNext = it }

        override fun next(): Row {
            val hasNext = itHasNext?.also { itHasNext = null } ?: this@iterator.moveToNext()
            if (!hasNext) throw NoSuchElementException("No more rows in Cursor.")
            return Row(this@iterator)
        }
    }
/**
 * Sets parameters on a [SQLiteStatement] for the provided list of parameters.
 * @param params The list of parameters to set on the [SQLiteStatement].
 */
fun SQLiteStatement.bindParameters(params: Array<out Any?>) {
    params.forEachIndexed { index, param ->
        when (param) {
            is String -> bindString(index + 1, param)
            is Int -> bindLong(index + 1, param.toLong())
            is Long -> bindLong(index + 1, param)
            is Float -> bindDouble(index + 1, param.toDouble())
            is Double -> bindDouble(index + 1, param)
            is BigDecimal -> bindString(index + 1, param.toString())
            is Short -> bindLong(index + 1, param.toLong())
            is Boolean -> bindLong(index + 1, if (param) 1 else 0)
            is ByteArray -> bindBlob(index + 1, param)
            is Byte -> bindLong(index + 1, param.toLong())
            null -> bindNull(index + 1)
            else -> bindString(index + 1, param.toString())
        }
    }
}
