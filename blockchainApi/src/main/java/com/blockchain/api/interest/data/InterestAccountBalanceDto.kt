package com.blockchain.api.interest.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InterestAccountBalanceDto(
    @SerialName("balance")
    val totalBalance: String,
    @SerialName("pendingInterest")
    val pendingInterest: String,
    @SerialName("pendingDeposit")
    val pendingDeposit: String,
    @SerialName("totalInterest")
    val totalInterest: String,
    @SerialName("pendingWithdrawal")
    val pendingWithdrawal: String,
    @SerialName("locked")
    val lockedBalance: String
)
