package com.blockchain.nabu.datamanagers.repositories.interest

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.service.NabuService
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import java.util.Calendar
import java.util.Date

interface InterestLimitsProvider {
    fun getLimitsForAllAssets(): Single<InterestLimitsList>
}

class InterestLimitsProviderImpl(
    private val assetCatalogue: AssetCatalogue,
    private val nabuService: NabuService,
    private val authenticator: Authenticator,
    private val currencyPrefs: CurrencyPrefs
) : InterestLimitsProvider {
    override fun getLimitsForAllAssets(): Single<InterestLimitsList> =
        authenticator.authenticate {
            nabuService.getInterestLimits(it, currencyPrefs.selectedFiatCurrency)
                .map { responseBody ->
                    InterestLimitsList(responseBody.limits.assetMap.entries.map { entry ->
                        val crypto = assetCatalogue.fromNetworkTicker(entry.key)!!

                        val minDepositFiatValue = FiatValue.fromMinor(
                            currencyPrefs.selectedFiatCurrency,
                            entry.value.minDepositAmount.toLong()
                        )
                        val maxWithdrawalFiatValue = FiatValue.fromMinor(
                            currencyPrefs.selectedFiatCurrency,
                            entry.value.maxWithdrawalAmount.toLong()
                        )

                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        calendar.add(Calendar.MONTH, 1)

                        InterestLimits(
                            interestLockUpDuration = entry.value.lockUpDuration,
                            minDepositFiatValue = minDepositFiatValue,
                            cryptoCurrency = crypto,
                            currency = entry.value.currency,
                            nextInterestPayment = calendar.time,
                            maxWithdrawalFiatValue = maxWithdrawalFiatValue
                        )
                    })
                }
        }
}

data class InterestLimits(
    val interestLockUpDuration: Int,
    val minDepositFiatValue: FiatValue,
    val cryptoCurrency: AssetInfo,
    val currency: String,
    val nextInterestPayment: Date,
    val maxWithdrawalFiatValue: FiatValue
)

data class InterestLimitsList(
    val list: List<InterestLimits>
)