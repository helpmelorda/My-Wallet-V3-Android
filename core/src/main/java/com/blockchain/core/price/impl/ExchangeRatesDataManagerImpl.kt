package com.blockchain.core.price.impl

import com.blockchain.api.services.AssetPriceService
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.androidcore.utils.extensions.then
import java.math.RoundingMode
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class ExchangeRatesDataManagerImpl(
    private val priceStore: AssetPriceStore,
    private val sparklineCall: SparklineCallCache,
    private val assetPriceService: AssetPriceService,
    private val currencyPrefs: CurrencyPrefs
) : ExchangeRatesDataManager {

    // TEMP Methods, for compatibility while changing client code
    @Synchronized
    override fun prefetchCache(assetList: List<AssetInfo>, fiatList: List<String>): Completable =
        priceStore.preloadCache(assetList, fiatList, currencyPrefs.selectedFiatCurrency)

    // We need to reload the cache if the user fiat has changed in this version of the implementation
    // This can go, down the road when we no longer fetch prices synchronously, but that's a big change
    // and out of scope for this PR. Balance calls need to be changed first
    @Synchronized
    override fun refetchCache(): Completable {
        sparklineCall.flush()
        return priceStore.populateCache(currencyPrefs.selectedFiatCurrency)
    }

    override fun getLastCryptoToUserFiatRate(
        sourceCrypto: AssetInfo
    ): ExchangeRate.CryptoToFiat {
        checkAndTriggerPriceRefresh()
        val userFiat = currencyPrefs.selectedFiatCurrency
        val price = priceStore.getCachedPrice(sourceCrypto, userFiat)
        return ExchangeRate.CryptoToFiat(
            from = sourceCrypto,
            to = userFiat,
            rate = price ?: throw IllegalStateException("Unknown crypto $sourceCrypto")
        )
    }

    override fun getLastCryptoToFiatRate(
        sourceCrypto: AssetInfo,
        targetFiat: String
    ): ExchangeRate.CryptoToFiat {
        checkAndTriggerPriceRefresh()
        val userFiat = currencyPrefs.selectedFiatCurrency
        return when {
            targetFiat == userFiat -> getLastCryptoToUserFiatRate(sourceCrypto)
            else -> getCryptoToFiatRate(sourceCrypto, targetFiat)
        }
    }

    override fun getLastFiatToCryptoRate(
        sourceFiat: String,
        targetCrypto: AssetInfo
    ): ExchangeRate.FiatToCrypto {
        checkAndTriggerPriceRefresh()
        val userFiat = currencyPrefs.selectedFiatCurrency
        return when {
            sourceFiat == userFiat -> getLastCryptoToUserFiatRate(targetCrypto).inverse()
            else -> getCryptoToFiatRate(targetCrypto, sourceFiat).inverse()
        }
    }

    private fun getCryptoToFiatRate(
        sourceCrypto: AssetInfo,
        targetFiat: String
    ): ExchangeRate.CryptoToFiat {
        checkAndTriggerPriceRefresh()
        // Target fiat should always be one of userFiat or in the "api" fiat list, so we should
        // always have it. TODO: Add some checking for this case
        val price = priceStore.getCachedPrice(sourceCrypto, targetFiat)
        return ExchangeRate.CryptoToFiat(
            from = sourceCrypto,
            to = targetFiat,
            rate = price ?: throw IllegalStateException("Unknown crypto $sourceCrypto")
        )
    }

    override fun getLastFiatToUserFiatRate(sourceFiat: String): ExchangeRate.FiatToFiat {
        checkAndTriggerPriceRefresh()
        val userFiat = currencyPrefs.selectedFiatCurrency
        val price = priceStore.getCachedFiatPrice(sourceFiat, userFiat)
        return ExchangeRate.FiatToFiat(
            from = sourceFiat,
            to = userFiat,
            rate = price ?: throw IllegalStateException("Unknown fiat $sourceFiat")
        )
    }

    override fun getLastFiatToFiatRate(sourceFiat: String, targetFiat: String): ExchangeRate.FiatToFiat {
        checkAndTriggerPriceRefresh()
        val userFiat = currencyPrefs.selectedFiatCurrency
        return when {
            sourceFiat == targetFiat -> ExchangeRate.FiatToFiat(
                from = sourceFiat,
                to = targetFiat,
                rate = 1.0.toBigDecimal()
            )
            targetFiat == userFiat -> getLastFiatToUserFiatRate(sourceFiat)
            sourceFiat == userFiat -> getLastFiatToUserFiatRate(targetFiat).inverse()
            else -> throw IllegalStateException("Unknown fiats $sourceFiat -> $targetFiat")
        }
    }

    private var refresher = AtomicReference<Disposable?>()

    @Synchronized
    private fun checkAndTriggerPriceRefresh() {
        // This should be in the price store, but it's a temp hack and can live here for now for simplicity
        // Ideally, we'd auto-refresh every minute, but we have no hook to know if the app
        // is BG or FG at this level, so we check on access and trigger a background refresh based on that.
        // once we have 'active prices' support upstack, hhich we'll need for full-dynamic,this can be removed.
        // And the checks themselves can trigger as required.
        // In this case we may have the occasional glitch, if requests to this cache are made across refreshes.
        // we know, however, that prices access tend to batch, so we can mitigate a little
        if (priceStore.shouldRefreshCache() && refresher.get() == null) {
            // I know. Ewwwww!
            refresher.set(
                Completable.timer(
                    AssetPriceStore.CACHE_REFRESH_DELAY_MILLIS,
                    TimeUnit.MILLISECONDS,
                    Schedulers.computation()
                ).then { priceStore.populateCache(currencyPrefs.selectedFiatCurrency) }
                    .subscribeBy { refresher.set(null) }
            )
        }
    }

    //    override fun getCurrentRate(fromAsset: AssetInfo, toFiat: String): Single<ExchangeRate> {
    //        return assetPriceService.getCurrentAssetPrice(fromAsset.ticker, toFiat)
    //            .map { price ->
    //                ExchangeRate.CryptoToFiat(
    //                    from = fromAsset,
    //                    to = toFiat,
    //                    rate = price.price.toBigDecimal()
    //                )
    //            }
    //    }
    //
    //    override fun getCurrentRateFiat(fromFiat: String, toFiat: String): Single<ExchangeRate> {
    //        return assetPriceService.getCurrentAssetPrice(fromFiat, toFiat)
    //            .map { price ->
    //                ExchangeRate.FiatToFiat(
    //                    from = fromFiat,
    //                    to = toFiat,
    //                    rate = price.price.toBigDecimal()
    //                )
    //            }
    //    }

    override fun getHistoricRate(
        fromAsset: AssetInfo,
        secSinceEpoch: Long
    ): Single<ExchangeRate> {
        val fiat = currencyPrefs.selectedFiatCurrency
        return assetPriceService.getHistoricPrice(
            crypto = fromAsset.ticker,
            fiat = fiat,
            time = secSinceEpoch
        ).map { price ->
            ExchangeRate.CryptoToFiat(
                from = fromAsset,
                to = fiat,
                rate = price.price.toBigDecimal()
            )
        }
    }

    override fun getPricesWith24hDelta(fromAsset: AssetInfo): Single<Prices24HrWithDelta> {
        val yesterday = (System.currentTimeMillis() / 1000) - SECONDS_PER_DAY

        // FIXME use the version on develop when merging this branch back
        return getHistoricRate(fromAsset, yesterday).map { priceYesterday ->
            val priceToday = getLastCryptoToUserFiatRate(fromAsset)
            Prices24HrWithDelta(
                delta24h = priceToday.priceDelta(priceYesterday),
                previousRate = priceYesterday,
                currentRate = priceToday
            )
        }
    }

    private fun ExchangeRate.priceDelta(previous: ExchangeRate): Double {
        return try {
            if (previous.rate.signum() != 0) {
                val current = rate
                val prev = previous.rate

                (current - prev)
                    .divide(prev, 4, RoundingMode.HALF_EVEN)
                    .movePointRight(2)
                    .toDouble()
            } else {
                Double.NaN
            }
        } catch (t: ArithmeticException) {
            Double.NaN
        }
    }

    override fun getHistoricPriceSeries(
        asset: AssetInfo,
        span: HistoricalTimeSpan,
        now: Calendar
    ): Single<HistoricalRateList> {
        require(asset.startDate != null)

        val scale = span.suggestTimescaleInterval()
        val startTime = now.getStartTimeForTimeSpan(span, asset)

        return assetPriceService.getHistoricPriceSince(
            crypto = asset.ticker,
            fiat = currencyPrefs.selectedFiatCurrency,
            start = startTime,
            scale = scale
        ).toHistoricalRateList()
    }

    override fun get24hPriceSeries(
        asset: AssetInfo
    ): Single<HistoricalRateList> =
        sparklineCall.fetch(asset, currencyPrefs.selectedFiatCurrency)

    override val fiatAvailableForRates: List<String>
        get() = priceStore.fiatQuoteTickers

    companion object {
        private const val SECONDS_PER_DAY = 24 * 60 * 60
    }
}
