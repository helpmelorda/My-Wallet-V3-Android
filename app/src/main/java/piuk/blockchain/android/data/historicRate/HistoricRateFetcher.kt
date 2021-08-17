package piuk.blockchain.android.data.historicRate

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

class HistoricRateFetcher(val localSource: HistoricRateLocalSource, val remoteSource: HistoricRateRemoteSource) {
    fun fetch(asset: AssetInfo, selectedFiat: String, timestampMs: Long, value: Money): Single<Money> {
        return localSource.get(selectedFiat, asset, timestampMs, value).onErrorResumeNext {
            remoteSource.get(asset, timestampMs, value).doOnSuccess {
                localSource.insert(selectedFiat, asset, timestampMs, it.toBigDecimal().toDouble())
            }
        }
    }
}