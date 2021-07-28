package com.blockchain.nabu.util.fakefactory.nabu

import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.LimitsJson
import com.blockchain.nabu.models.responses.nabu.TierLevels
import com.blockchain.nabu.models.responses.nabu.TierResponse

object FakeTierLevelsFactory {
    val any = TierLevels(0, 1, 2)
}

object FakeKycTiersFactory {
    val any = KycTiers(
        arrayListOf(
            FakeTiersResponseFactory.tierZero,
            FakeTiersResponseFactory.tierOne,
            FakeTiersResponseFactory.tierTwo
        )
    )
}

object FakeTiersResponseFactory {
    val tierZero = TierResponse(
        0,
        "Tier 0",
        state = KycTierState.Verified,
        limits = LimitsJson(
            currency = "USD",
            daily = null,
            annual = null
        )
    )

    val tierOne = TierResponse(
        1,
        "Tier 1",
        state = KycTierState.Pending,
        limits = LimitsJson(
            currency = "USD",
            daily = null,
            annual = 1000.0.toBigDecimal()
        )
    )
    val tierTwo = TierResponse(
        2,
        "Tier 2",
        state = KycTierState.None,
        limits = LimitsJson(
            currency = "USD",
            daily = 25000.0.toBigDecimal(),
            annual = null
        )
    )
}