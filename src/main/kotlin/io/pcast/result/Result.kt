package io.pcast.result

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class Result<out V : Any?, out E : Exception> {
    open operator fun component1(): V? = null
    open operator fun component2(): E? = null

    abstract fun get(): V
    abstract fun error(): E

    class Ok<out V : Any?>(
        val value: V
    ) : Result<V, Nothing>() {
        override operator fun component1(): V = value

        override fun get(): V = value

        override fun error() = throw IllegalArgumentException()

        override fun toString() = "Ok: $value"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            return other is Ok<*> && value == other.value
        }

        override fun hashCode() = value?.hashCode() ?: 0
    }

    class Error<out E : Exception>(
        val error: E
    ) : Result<Nothing, E>() {
        override fun component2(): E = error

        override fun get() = throw error

        override fun error() = error

        override fun toString() = "Error: $error"
        override fun hashCode() = error.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            return other is Error<*> && error == other.error
        }
    }

    companion object {
        fun ok(): Ok<Unit> = Ok(Unit)
        fun <V : Any?> ok(value: V): Ok<V> = Ok(value)
        fun <E : Exception> error(error: E): Result<Nothing, E> = Error(error)
        fun error(): Error<Exception> = Error(Exception())
    }
}

infix fun <V : Any?, E : Exception> Result<V, E>.or(
    fallback: V
) = if (isOk()) value else fallback

inline infix fun <V : Any?, E : Exception> Result<V, E>.or(
    callback: (E) -> V
) = if (isOk()) value else callback(error)

fun <V : Any?, E : Exception> Result<V, E>.orNull(): V? = or(null)

inline infix fun <V : Any?, E : Exception> Result<V, E>.onOk(
    callback: (V) -> Unit
) = if (isOk()) callback(value) else Unit

fun <V : Any?, E : Exception> Result<V, E>.unwrap(): V = or { throw it }

fun <V : Any?, E : Exception, R> Result<V, E>.unwrap(transform: (r: V) -> R): R =
    if (isOk()) transform(value) else throw error

inline fun <T> attempt(callback: () -> T) = try {
    Result.ok(callback())
} catch (e: Exception) {
    Result.error(e)
}

@OptIn(ExperimentalContracts::class)
fun <V, E : Exception> Result<V, E>.isOk(): Boolean {
    contract {
        returns(true) implies (this@isOk is Result.Ok)
        returns(false) implies (this@isOk is Result.Error)
    }

    return this is Result.Ok
}