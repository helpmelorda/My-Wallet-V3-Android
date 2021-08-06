package com.blockchain.nabu.datamanagers.repositories.interest

import com.blockchain.nabu.Authenticator
import com.blockchain.api.services.NabuUserService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import java.lang.IllegalArgumentException

enum class IneligibilityReason {
    INVALID_USER,
    REGION,
    KYC_TIER,
    BLOCKED,
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
    try {
        when {
            this.isEmpty() -> IneligibilityReason.NONE
            else -> IneligibilityReason.valueOf(this)
        }
    } catch (t: IllegalArgumentException) {
        IneligibilityReason.OTHER
    }