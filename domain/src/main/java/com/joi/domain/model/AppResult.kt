package com.joi.domain.model

/** Mirrors the backend's AppError shape ({error: CODE, message}), carried across the network boundary. */
data class AppError(
    val code: String,
    val message: String,
)

/**
 * Every repository/use-case call returns this instead of throwing, so every screen's ViewModel
 * handles failure the same explicit way (no try/catch scattered through the UI layer).
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}
