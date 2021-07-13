package info.blockchain.wallet.prices

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.ExchangeRate
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit

internal fun CurrentPriceApi.toIndicativeFiatPriceService(): IndicativeFiatPriceService {
    return CurrentPriceApiIndicativeFiatPriceServiceAdapter(this)
}

private class CurrentPriceApiIndicativeFiatPriceServiceAdapter(
    private val currentPriceApi: CurrentPriceApi
) : IndicativeFiatPriceService {

    override fun indicativeRateStream(from: AssetInfo, toFiat: String): Observable<ExchangeRate.CryptoToFiat> =
        Observable.defer {
            currentPriceApi.currentPrice(from, toFiat).toObservable()
        }
            .repeatWhen { it.delay(1, TimeUnit.SECONDS) }
            .retryWhen { it.delay(1, TimeUnit.SECONDS) }
            .map {
                ExchangeRate.CryptoToFiat(
                    from,
                    toFiat,
                    it
                )
            }
}
