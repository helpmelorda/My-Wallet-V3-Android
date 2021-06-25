package com.blockchain.api

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.functions.Function
import retrofit2.HttpException

open class ApiException : Exception {

    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(cause: Throwable) : super(cause)

    protected constructor(
        message: String,
        cause: Throwable,
        enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(
        message,
        cause,
        enableSuppression,
        writableStackTrace
    )

    val isMailNotVerifiedException: Boolean
        get() = message == "Email is not verified."
}

// This is an interim method, until we move the rest of the Nabu API over to this module
internal fun <T> Single<T>.wrapErrorMessage(): Single<T> = this.onErrorResumeNext {
    when (it) {
        is HttpException -> Single.error(ApiException(it))
        else -> Single.error(it)
    }
}

internal fun <T> Maybe<T>.wrapErrorMessage(): Maybe<T> = this.onErrorResumeNext(
    Function {
        when (it) {
            is HttpException -> Maybe.error(ApiException(it))
            else -> Maybe.error(it)
        }
    }
)
