package com.blockchain.data.activity.historicRate

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single

class HistoricRateFetcher(val localSource: HistoricRateLocalSource, val remoteSource: HistoricRateRemoteSource) {
    fun fetch(asset: AssetInfo, selectedFiat: String, timestampMs: Long, value: Money): Single<Money> {
        return localSource.get(selectedFiat, asset, timestampMs).onErrorResumeNext {
            remoteSource.get(asset, timestampMs).doOnSuccess {
                localSource.insert(selectedFiat, asset, timestampMs, it.price().toBigDecimal().toDouble())
            }
        }.map {
            it.convert(value)
        }
    }
}