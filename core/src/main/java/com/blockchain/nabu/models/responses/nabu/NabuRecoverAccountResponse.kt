package com.blockchain.nabu.models.responses.nabu

import retrofit2.http.Field

data class NabuRecoverAccountResponse(
    @Field("token")
    val token: String
)