package com.blockchain.api.services

import com.blockchain.api.auth.AuthApiInterface
import com.blockchain.api.auth.data.SendEmailRequest
import io.reactivex.rxjava3.core.Completable

class AuthApiService internal constructor(
    private val api: AuthApiInterface,
    private val apiCode: String,
    private val captchaSiteKey: String
) {
    fun sendEmailForAuthentication(sessionId: String, email: String, captcha: String): Completable {
        return api.sendEmailForAuthentication(
            sessionId.withBearerPrefix(),
            SendEmailRequest(
            apiCode,
            email,
            captcha,
            PRODUCT_WALLET,
            captchaSiteKey
            )
        )
    }

    companion object {
        private const val PRODUCT_WALLET = "wallet"
    }

    private fun String.withBearerPrefix() = "Bearer $this"
}