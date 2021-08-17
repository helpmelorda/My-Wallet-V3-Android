package piuk.blockchain.android.data.historicRate

import com.blockchain.core.price.ExchangeRatesDataManager
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single

class HistoricRateRemoteSource(private val exchangeRates: ExchangeRatesDataManager) {
    fun get(asset: AssetInfo, timeStampMs: Long, value: Money): Single<Money> {
        return exchangeRates.getHistoricRate(
            fromAsset = asset,
            secSinceEpoch = timeStampMs / 1000 // API uses seconds
        ).map {
            it.convert(value)
        }
    }
}