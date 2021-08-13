package com.blockchain.api.nabu.data

import kotlinx.serialization.Serializable

@Serializable
data class GeolocationResponse(
    val ip: String,
    val countryCode: String,
    val state: String? = null
)