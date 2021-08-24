package com.blockchain.api.nabu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InitialAddressRequest(
    @SerialName("country")
    val country: String,
    @SerialName("state")
    val state: String?
)