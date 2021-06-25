package piuk.blockchain.androidcore.data.exchangerate

import info.blockchain.balance.AssetInfo
import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import java.util.Calendar

enum class TimeSpan {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME
}

/**
 * All time start times in epoch-seconds
 */

typealias PriceSeries = List<PriceDatum>

class ExchangeRateService(private val priceApi: PriceApi, rxBus: RxBus) {
    private val rxPinning = RxPinning(rxBus)

    fun getExchangeRateMap(asset: AssetInfo): Single<Map<String, PriceDatum>> =
        priceApi.getPriceIndexes(asset.ticker)

    fun getHistoricPrice(
        asset: AssetInfo,
        fiatCurrency: String,
        timeInSeconds: Long
    ): Single<Double> =
        priceApi.getHistoricPrice(asset.ticker, fiatCurrency, timeInSeconds)

    fun getHistoricPriceSeries(
        asset: AssetInfo,
        fiatCurrency: String,
        timeSpan: TimeSpan,
        timeInterval: TimeInterval = suggestedTimeIntervalForSpan(timeSpan)
    ): Single<PriceSeries> {
        require(asset.startDate != null)

        var proposedStartTime = getStartTimeForTimeSpan(timeSpan, asset)
        // It's possible that the selected start time is before the currency existed, so check here
        // and show ALL_TIME instead if that's the case.
        if (proposedStartTime < asset.startDate!!) {
            proposedStartTime = getStartTimeForTimeSpan(TimeSpan.ALL_TIME, asset)
        }

        return rxPinning.callSingle {
            priceApi.getHistoricPriceSeries(
                asset.ticker,
                fiatCurrency,
                proposedStartTime,
                timeInterval.intervalSeconds
            ).subscribeOn(Schedulers.io())
        }
    }

    /**
     * Provides the first timestamp for which we have prices, returned in epoch-seconds
     *
     */
    private fun getStartTimeForTimeSpan(
        timeSpan: TimeSpan,
        asset: AssetInfo
    ): Long {
        require(asset.startDate != null)
        val start = when (timeSpan) {
            TimeSpan.ALL_TIME -> return asset.startDate!!
            TimeSpan.YEAR -> 365
            TimeSpan.MONTH -> 30
            TimeSpan.WEEK -> 7
            TimeSpan.DAY -> 1
        }

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -start) }
        return cal.timeInMillis / 1000
    }

    private fun suggestedTimeIntervalForSpan(timeSpan: TimeSpan): TimeInterval =
        when (timeSpan) {
            TimeSpan.ALL_TIME -> TimeInterval.FIVE_DAYS
            TimeSpan.YEAR -> TimeInterval.ONE_DAY
            TimeSpan.MONTH -> TimeInterval.TWO_HOURS
            TimeSpan.WEEK -> TimeInterval.ONE_HOUR
            TimeSpan.DAY -> TimeInterval.FIFTEEN_MINUTES
        }
}