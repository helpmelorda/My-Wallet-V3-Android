package com.blockchain.api.services

import com.blockchain.api.nabu.NabuUserApi
import com.blockchain.api.nabu.data.InterestEligibilityResponse
import io.reactivex.rxjava3.core.Single
import java.util.Locale

data class InterestEligibility(
    private val eligibilityMap: Map<String, InterestEligibilityResponse>
) {
    fun getEligibleFor(assetTicker: String): InterestEligibilityResponse {
        return eligibilityMap[assetTicker.toUpperCase(Locale.ROOT)] ?: InterestEligibilityResponse(
            isEligible = false,
            reason = InterestEligibilityResponse.DEFAULT_FAILURE_REASON
        )
    }
}

// TODO: Add nabu User and User Capability calls to this service (and the underlying interface)
class NabuUserService internal constructor(
    private val api: NabuUserApi
) {
    fun getInterestEligibility(
        authHeader: String
    ): Single<InterestEligibility> =
        api.getInterestEligibility(authorization = authHeader)
            .onErrorReturn { emptyMap() }
            .map { it.toDomain() }
}

private fun Map<String, InterestEligibilityResponse>.toDomain(): InterestEligibility =
    InterestEligibility(this)
