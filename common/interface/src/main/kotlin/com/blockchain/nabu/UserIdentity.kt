package com.blockchain.nabu

import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.io.Serializable

interface UserIdentity {
    fun isEligibleFor(feature: Feature): Single<Boolean>
    fun isVerifiedFor(feature: Feature): Single<Boolean>
    fun isKycInProgress(): Single<Boolean>
    fun isKycResubmissionRequired(): Single<Boolean>
    fun shouldResubmitAfterRecovery(): Single<Boolean>
    fun getBasicProfileInformation(): Single<BasicProfileInfo>
    fun checkForUserWalletLinkErrors(): Completable
}

sealed class Feature {
    data class TierLevel(val tier: Tier) : Feature()
    object SimplifiedDueDiligence : Feature()
    data class Interest(val currency: AssetInfo) : Feature()
    object SimpleBuy : Feature()
}

enum class Tier {
    BRONZE, SILVER, GOLD
}

data class BasicProfileInfo(
    val firstName: String,
    val lastName: String,
    val email: String
) : Serializable