package com.blockchain.nabu.datamanagers.repositories.interest

import com.blockchain.api.services.NabuUserService
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.datamanagers.repositories.interest.InterestEligibilityProviderImpl.Companion.ELIGIBLE
import com.blockchain.nabu.datamanagers.repositories.interest.InterestEligibilityProviderImpl.Companion.INVALID_ADDRESS
import com.blockchain.nabu.datamanagers.repositories.interest.InterestEligibilityProviderImpl.Companion.TIER_TOO_LOW
import com.blockchain.nabu.datamanagers.repositories.interest.InterestEligibilityProviderImpl.Companion.UNSUPPORTED_REGION
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

enum class IneligibilityReason {
    REGION,
    KYC_TIER,
    OTHER,
    NONE
}

interface InterestEligibilityProvider {
    fun getEligibilityForCustodialAssets(): Single<List<AssetInterestEligibility>>
}

class InterestEligibilityProviderImpl(
    private val assetCatalogue: AssetCatalogue,
    private val nabuService: NabuUserService,
    private val authenticator: Authenticator
) : InterestEligibilityProvider {
    override fun getEligibilityForCustodialAssets(): Single<List<AssetInterestEligibility>> =
        authenticator.authenticate { token ->
            nabuService.getInterestEligibility(token.authHeader)
                .map { response ->
                    assetCatalogue.supportedCustodialAssets
                        .map { asset ->
                            val eligible = response.getEligibleFor(asset.ticker)
                            AssetInterestEligibility(
                                asset,
                                Eligibility(
                                    eligible = eligible.isEligible,
                                    ineligibilityReason = eligible.reason.toReason()
                                )
                            )
                        }
                }
        }

    companion object {
        const val UNSUPPORTED_REGION = "UNSUPPORTED_REGION"
        const val TIER_TOO_LOW = "TIER_TOO_LOW"
        const val INVALID_ADDRESS = "INVALID_ADDRESS"
        const val ELIGIBLE = "NONE"
    }
}

data class AssetInterestEligibility(
    val cryptoCurrency: AssetInfo,
    val eligibility: Eligibility
)

data class Eligibility(
    val eligible: Boolean,
    val ineligibilityReason: IneligibilityReason
) {
    companion object {
        fun notEligible() = Eligibility(false, IneligibilityReason.OTHER)
    }
}

private fun String.toReason(): IneligibilityReason =
    when {
        this.isEmpty() -> IneligibilityReason.NONE
        this == ELIGIBLE -> IneligibilityReason.NONE
        this == UNSUPPORTED_REGION -> IneligibilityReason.REGION
        this == INVALID_ADDRESS -> IneligibilityReason.REGION
        this == TIER_TOO_LOW -> IneligibilityReason.KYC_TIER
        else -> IneligibilityReason.OTHER
    }