package com.blockchain.nabu.models.responses.nabu

data class ResubmissionResponse(
    val reason: Int,
    val details: String
) {
    companion object {
        const val ACCOUNT_RECOVERED_REASON = 1
    }
}