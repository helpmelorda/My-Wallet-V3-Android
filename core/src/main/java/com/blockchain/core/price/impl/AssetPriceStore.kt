package com.blockchain.core.price.impl

import com.blockchain.api.services.AssetPrice
import com.blockchain.api.services.AssetPriceService
import com.blockchain.api.services.AssetSymbol
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import timber.log.Timber
import java.math.BigDecimal

private data class AssetPriceRecord(
    val base: String,
    val quote: String,
    val rate: Double,
    val fetchedAt: Long
)

private data class AssetPair(
    val base: String,
    val quote: String
)

// The price api BE refreshes it's cache every minute, so there's no point refreshing ours more than that.
class AssetPriceStore(
    private val assetPriceService: AssetPriceService
) {
    private lateinit var supportedBases: List<AssetSymbol>
    private lateinit var supportedQuotes: List<AssetSymbol>

    // TEMP to track which assets we are requesting prices for. When this store becomes active, and
    // active - ie async - prices are supported upstack, the refresh mechanism will change and we won't need this
    // but while assets are hardcoded to a known, limited set, we can be dumb and use this for cache refreshes
    private val cachedBaseAssetList: MutableList<AssetInfo> = mutableListOf()
    private val cachedBaseFiatList: MutableList<String> = mutableListOf()

    private val priceMap: MutableMap<AssetPair, AssetPriceRecord> = mutableMapOf()

    @Deprecated("TEMP For semi-dynamic assets ONLY")
    fun preloadCache(baseAssets: List<AssetInfo>, baseFiat: List<String>, quoteFiat: String): Completable {
        cachedBaseAssetList.clear()
        cachedBaseAssetList.addAll(baseAssets)
        cachedBaseFiatList.clear()
        cachedBaseFiatList.addAll(baseFiat)

        return assetPriceService.getSupportedCurrencies()
            .doOnSuccess { symbols ->
                supportedBases = symbols.base
                supportedQuotes = symbols.quote
            }.flatMapCompletable {
                populateCache(quoteFiat)
            }
    }

    @Deprecated("TEMP For semi-dynamic assets ONLY")
    fun populateCache(quoteFiat: String): Completable {
        val allBase = cachedBaseAssetList.map { it.ticker } + cachedBaseFiatList
        val allQuote = cachedBaseFiatList.toSet() + quoteFiat

        return Single.zip(
            allQuote.map { fiat ->
                assetPriceService.getCurrentPrices(
                    cryptoTickerList = allBase.toSet(),
                    fiat = fiat
                ).doOnSuccess { prices -> updateCachedData(prices, fiat) }
                    .doOnError { Timber.d("Nope") }
            }
        ) { /* Empty */ }
        .doOnError {
            Timber.e("Failed to get prices")
        }.doOnSuccess {
            Timber.d("Cache loaded")
        }.ignoreElement()
    }

    @Synchronized
    private fun updateCachedData(prices: Map<String, AssetPrice>, fiat: String) {
        val now = System.currentTimeMillis()
        prices.forEach {
            val pair = AssetPair(it.key, fiat)
            priceMap[pair] = AssetPriceRecord(
                base = it.key,
                quote = fiat,
                rate = it.value.price,
                fetchedAt = now
            )
        }
    }

    @Synchronized
    fun getCachedPrice(fromAsset: AssetInfo, toFiat: String): BigDecimal? {
        val pair = AssetPair(fromAsset.ticker, toFiat)

        return priceMap[pair]?.let { record ->
            check(toFiat == record.quote) { "Bad fiat! $toFiat" }
            record.rate.toBigDecimal()
        }
    }

    @Synchronized
    fun getCachedFiatPrice(fromFiat: String, toFiat: String): BigDecimal? {
        val pair = AssetPair(fromFiat, toFiat)

        return priceMap[pair]?.let { record ->
            check(toFiat == record.quote) { "Bad fiat! $toFiat" }
            record.rate.toBigDecimal()
        } ?: throw IllegalStateException("Unknown fiat $fromFiat")
    }

    @Synchronized
    internal fun shouldRefreshCache(): Boolean {
        // The BE prices cache is valid for one minute.
        // Since price fetches are batched, we don't need to walk the list, the first one should be enough
        return System.currentTimeMillis() - priceMap.values.first().fetchedAt > CACHE_REFRESH_INTERVAL_MILLIS
    }

    @Synchronized
    fun hasPriceFor(fromAsset: AssetInfo, toFiat: String): Boolean =
        priceMap[AssetPair(fromAsset.ticker, toFiat)] != null

    val fiatQuoteTickers: List<String>
        get() = supportedQuotes.filter { it.isFiat }.map { it.ticker }

    companion object {
        private const val CACHE_REFRESH_INTERVAL_MILLIS = 59 * 1000L
        internal const val CACHE_REFRESH_DELAY_MILLIS = 2 * 1000L
    }
}
