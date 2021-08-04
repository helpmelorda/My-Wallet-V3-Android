package com.blockchain.core.price.impl

import com.blockchain.api.services.AssetPriceService
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.rx.ParameteredMappedSinglesTimedRequests
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import java.util.Calendar
import java.util.concurrent.atomic.AtomicReference

internal class SparklineCallCache(
    private val priceService: AssetPriceService
) {
    private val cacheRequest: ParameteredMappedSinglesTimedRequests<AssetInfo, HistoricalRateList> by lazy {
        ParameteredMappedSinglesTimedRequests(
            cacheLifetimeSeconds = SPARKLINE_CACHE_TTL_SECONDS,
            refreshFn = ::refreshCache
        )
    }

    private val userFiat = AtomicReference<String>()

    private fun refreshCache(asset: AssetInfo): Single<HistoricalRateList> {
        val span = HistoricalTimeSpan.DAY
        val scale = span.suggestTimescaleInterval()
        val startTime = Calendar.getInstance().getStartTimeForTimeSpan(span, asset)

        return priceService.getHistoricPriceSince(
            crypto = asset.ticker,
            fiat = userFiat.get(),
            start = startTime,
            scale = scale
        ).toHistoricalRateList()
    }

    fun fetch(asset: AssetInfo, userFiat: String): Single<HistoricalRateList> {
        val oldUserFiat = this.userFiat.getAndSet(userFiat)
        if (oldUserFiat != userFiat) {
            cacheRequest.invalidateAll()
        }
        return cacheRequest.getCachedSingle(asset)
    }

    fun flush() {
        cacheRequest.invalidateAll()
    }

    companion object {
        private const val SPARKLINE_CACHE_TTL_SECONDS = 5 * 16L // 5 minutes
    }
}
