package com.example.campusconnect.data.repository

import kotlin.coroutines.cancellation.CancellationException

/** Like runCatching, but lets coroutine cancellation through instead of swallowing it. */
internal inline fun <T> attempt(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

/** Thrown when something can't be deleted because users still point at it. */
class InUseException(message: String) : Exception(message)

/** Thrown when the input is rejected, e.g. a code that's already taken. */
class InvalidInputException(message: String) : Exception(message)