sealed class DatabaseError {
    data class ExecutionError(val sql: String, val args: Array<out Any?>, val message: String) : DatabaseError()
    data class BatchExecutionError(val sql: String, val message: String) : DatabaseError()
    data class QueryError(val sql: String, val args: Array<out Any?>, val message: String) : DatabaseError()
    data class TransactionError(val message: String, val cause: Throwable? = null) : DatabaseError()
    data object UnknownError : DatabaseError()
}