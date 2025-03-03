sealed class DatabaseResult<out T> {
    data class Success<T>(@PublishedApi internal val value: T) : DatabaseResult<T>()
    data class Failure(@PublishedApi internal val error: DatabaseError) : DatabaseResult<Nothing>()

    val isSuccess: Boolean  get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun getErrorOrNull(): DatabaseError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    inline fun <R> map(transform: (T) -> R): DatabaseResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(error)
    }

    inline fun onSuccess(action: (T) -> Unit): DatabaseResult<T> {
        if (this is Success) action(value)
        return this
    }

    inline fun onFailure(action: (DatabaseError) -> Unit): DatabaseResult<T> {
        if (this is Failure) action(error)
        return this
    }

    companion object {
        suspend inline fun <T> runCatching(
            errorTransform: (Throwable) -> DatabaseError = { DatabaseError.UnknownError },
            crossinline block: suspend () -> T
        ): DatabaseResult<T> {
            return try {
                Success(block())
            } catch (e: Throwable) {
                Failure(errorTransform(e))
            }
        }
    }
}
