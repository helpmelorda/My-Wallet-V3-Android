package com.blockchain.nabu.models.responses.nabu

import retrofit2.http.Field

data class NabuRecoverAccountRequest(
    val jwt: String,
    @Field("recovery_token")
    val recoveryToken: String
)