package com.blockchain.api.nabu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeolocationResponse(
    val ip: String,
    @SerialName("countryCode")
    val countryCode: String,
    @SerialName("state")
    val state: String? = null
)