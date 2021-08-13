package com.blockchain.nabu

import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

// We are going to use a fake instance instead of a mock because it is easier to
// override this generics methods than use a Mockito's ArgumentCaptor.
//
class FakeAuthenticator(private val sessionToken: NabuSessionTokenResponse) : Authenticator {
    override fun <T> authenticate(singleFunction: (NabuSessionTokenResponse) -> Single<T>): Single<T> =
        Single.just(sessionToken).flatMap { singleFunction(it) }

    override fun <T> authenticateMaybe(maybeFunction: (NabuSessionTokenResponse) -> Maybe<T>): Maybe<T> {
        throw Exception("Not expected")
    }

    override fun <T> authenticateSingle(singleFunction: (Single<NabuSessionTokenResponse>) -> Single<T>): Single<T> {
        return singleFunction(Single.just(sessionToken))
    }

    override fun invalidateToken() {
        throw Exception("Not expected")
    }

    override fun getAuthHeader(): Single<String> {
        return Single.just(sessionToken.authHeader)
    }
}