package com.blockchain.api.auth

import com.blockchain.api.auth.data.SendEmailRequest
import io.reactivex.rxjava3.core.Completable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface AuthApiInterface {
    @POST("auth/email-reminder")
    fun sendEmailForAuthentication(
        @Header("Authorization") sessionId: String,
        @Body request: SendEmailRequest
    ): Completable
}