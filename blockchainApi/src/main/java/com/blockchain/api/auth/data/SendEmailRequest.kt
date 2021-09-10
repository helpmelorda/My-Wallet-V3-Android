package com.blockchain.api.auth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendEmailRequest(
    @SerialName("api_code") val apiCode: String,
    @SerialName("email") val email: String,
    @SerialName("captcha") val captcha: String,
    @SerialName("product") val product: String,
    @SerialName("siteKey") val siteKey: String
)