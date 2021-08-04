package com.blockchain.core.price.impl

import com.blockchain.api.services.AssetPrice
import com.blockchain.api.services.PriceTimescale
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import java.util.Calendar

/**
 * Provides the first timestamp for which we have prices, returned in epoch-seconds
 */
internal fun Calendar.getStartTimeForTimeSpan(
    timeSpan: HistoricalTimeSpan,
    asset: AssetInfo
): Long {
    val assetStartDate = asset.startDate
    require(assetStartDate != null)

    val start = when (timeSpan) {
        HistoricalTimeSpan.ALL_TIME -> return assetStartDate
        HistoricalTimeSpan.YEAR -> 365
        HistoricalTimeSpan.MONTH -> 30
        HistoricalTimeSpan.WEEK -> 7
        HistoricalTimeSpan.DAY -> 1
    }

    val cal = apply { add(Calendar.DAY_OF_YEAR, -start) }

    val startTime = cal.timeInMillis / 1000
    // It's possible that the selected start time is before the currency existed, so check here
    // and show ALL_TIME instead if that's the case.
    return if (startTime < assetStartDate) {
        getStartTimeForTimeSpan(HistoricalTimeSpan.ALL_TIME, asset)
    } else {
        startTime
    }
}

internal fun HistoricalTimeSpan.suggestTimescaleInterval(): PriceTimescale =
    when (this) {
        HistoricalTimeSpan.ALL_TIME -> PriceTimescale.FIVE_DAYS
        HistoricalTimeSpan.YEAR -> PriceTimescale.ONE_DAY
        HistoricalTimeSpan.MONTH -> PriceTimescale.TWO_HOURS
        HistoricalTimeSpan.WEEK -> PriceTimescale.ONE_HOUR
        HistoricalTimeSpan.DAY -> PriceTimescale.FIFTEEN_MINUTES
    }

internal fun Single<List<AssetPrice>>.toHistoricalRateList() =
    this.map { list ->
        list.map {
            HistoricalRate(
                it.timestamp,
                it.price
            )
        }
    }