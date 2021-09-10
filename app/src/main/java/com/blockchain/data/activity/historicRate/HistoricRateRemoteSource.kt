package com.blockchain.data.activity.historicRate

import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class HistoricRateRemoteSource(private val exchangeRates: ExchangeRatesDataManager) {
    fun get(asset: AssetInfo, timeStampMs: Long): Single<ExchangeRate> {
        return exchangeRates.getHistoricRate(
            fromAsset = asset,
            secSinceEpoch = timeStampMs / 1000 // API uses seconds
        )
    }
}