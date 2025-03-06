import java.math.BigDecimal

data class TestEntity(
    val pkId: Int,
    val stringValue: String? = null,
    val intValue: Int? = null,
    val longValue: Long? = null,
    val floatValue: Float? = null,
    val doubleValue: Double? = null,
    val bigDecimalValue: BigDecimal? = null,
    val shortValue: Short? = null,
    val booleanValue: Boolean? = null,
    val byteArrayValue: ByteArray? = null,
    val byteValue: Byte? = null,
    val nullValue: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestEntity) return false

        return pkId == other.pkId &&
                stringValue == other.stringValue &&
                intValue == other.intValue &&
                longValue == other.longValue &&
                floatValue == other.floatValue &&
                doubleValue == other.doubleValue &&
                bigDecimalValue == other.bigDecimalValue &&
                shortValue == other.shortValue &&
                booleanValue == other.booleanValue &&
                byteArrayValue.contentEquals(other.byteArrayValue) &&
                byteValue == other.byteValue &&
                nullValue == other.nullValue
    }

    override fun hashCode(): Int {
        var result = pkId
        result = 31 * result + stringValue.hashCode()
        result = 31 * result + (intValue ?: 0)
        result = 31 * result + longValue.hashCode()
        result = 31 * result + floatValue.hashCode()
        result = 31 * result + doubleValue.hashCode()
        result = 31 * result + bigDecimalValue.hashCode()
        result = 31 * result + (shortValue ?: 0)
        result = 31 * result + booleanValue.hashCode()
        result = 31 * result + byteArrayValue.contentHashCode()
        result = 31 * result + (byteValue ?: 0)
        result = 31 * result + (nullValue?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun createTable() = """
            CREATE TABLE test_entity (
                pkId INTEGER PRIMARY KEY,
                stringValue TEXT,
                intValue INTEGER,
                longValue INTEGER,
                floatValue REAL,
                doubleValue REAL,
                bigDecimalValue TEXT,
                shortValue INTEGER,
                booleanValue INTEGER,
                byteArrayValue BLOB,
                byteValue INTEGER,
                nullValue TEXT
                )
            """
    }
}