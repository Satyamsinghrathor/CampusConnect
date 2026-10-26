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
