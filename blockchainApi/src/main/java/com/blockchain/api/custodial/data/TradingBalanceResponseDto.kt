package com.blockchain.api.custodial.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TradingBalanceResponseDto(
    @SerialName("pending")
    val pending: String,
    @SerialName("available") // Badly named param, is actually the total including uncleared & locked
    val total: String,
    @SerialName("withdrawable") // Balance that is NOT uncleared and IS withdrawable
    val actionable: String
)
