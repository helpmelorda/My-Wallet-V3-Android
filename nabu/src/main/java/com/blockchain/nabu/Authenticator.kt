package com.blockchain.nabu

import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

interface Authenticator {

    fun <T> authenticate(
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): Single<T>

    fun <T> authenticateMaybe(
        maybeFunction: (NabuSessionTokenResponse) -> Maybe<T>
    ): Maybe<T>

    fun <T> authenticateSingle(
        singleFunction: (Single<NabuSessionTokenResponse>) -> Single<T>
    ): Single<T>

    fun authenticateCompletable(
        completableFunction: (NabuSessionTokenResponse) -> Completable
    ): Completable = authenticate {
        completableFunction(it)
            .toSingleDefault(Unit)
    }.ignoreElement()

    fun authenticate(): Single<NabuSessionTokenResponse> =
        authenticateSingle { it }

    fun invalidateToken()
}
