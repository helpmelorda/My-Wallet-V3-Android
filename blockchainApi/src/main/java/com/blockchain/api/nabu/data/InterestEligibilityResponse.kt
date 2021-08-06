package com.blockchain.api.nabu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InterestEligibilityResponse(
    @SerialName("eligible")
    val isEligible: Boolean = false,
    @SerialName("ineligibilityReason")
    val reason: String = DEFAULT_REASON_NONE
) {
    companion object {
        internal const val DEFAULT_REASON_NONE = "NONE"
        internal const val DEFAULT_FAILURE_REASON = "OTHER"
    }
}
