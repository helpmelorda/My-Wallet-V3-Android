package com.blockchain.api.services

import com.blockchain.api.assetprice.AssetPriceApiInterface
import com.blockchain.api.assetprice.data.AssetPriceDto
import com.blockchain.api.assetprice.data.PriceRequestPairDto
import com.blockchain.api.assetprice.data.PriceSymbolDto
import io.reactivex.rxjava3.core.Single

enum class PriceTimescale(val intervalSeconds: Int) {
    FIFTEEN_MINUTES(900),
    ONE_HOUR(3600),
    TWO_HOURS(7200),
    ONE_DAY(86400),
    FIVE_DAYS(432000)
}

data class AssetSymbol(
    val ticker: String,
    val name: String,
    val precisionDp: Int,
    val isFiat: Boolean
)

data class SupportedAssetSymbols(
    val base: List<AssetSymbol>,
    val quote: List<AssetSymbol>
)

data class AssetPrice(
    val price: Double,
    val timestamp: Long
)

class AssetPriceService internal constructor(
    private val api: AssetPriceApiInterface,
    private val apiCode: String
) {
    /** All the symbols supported by the price API */
    fun getSupportedCurrencies(): Single<SupportedAssetSymbols> =
        api.getAvailableSymbols(apiCode)
            .map { dto ->
                SupportedAssetSymbols(
                    base = dto.baseSymbols.values.map { it.toAssetSymbol() },
                    quote = dto.quoteSymbols.values.map { it.toAssetSymbol() }
                )
            }

    /** Get the current price index of a single currency pair */
    fun getCurrentAssetPrice(
        crypto: String,
        fiat: String
    ): Single<AssetPrice> =
        api.getCurrentPrices(
            pairs = listOf(
                PriceRequestPairDto(
                    crypto = crypto,
                    fiat = fiat
                )
            ),
            apiKey = apiCode
        ).map { result ->
            result.map {
                it.key.extractCryptoTicker() to it.value.toAssetPrice()
            }.first()
        }.map {
            check(it.first == crypto)
            it.second
        }

    /** Get the current or at a specific time price index of a single currency pair */
    fun getHistoricPrice(
        crypto: String,
        fiat: String,
        time: Long
    ): Single<AssetPrice> =
        api.getHistoricPrices(
            pairs = listOf(
                PriceRequestPairDto(
                    crypto = crypto,
                    fiat = fiat
                )
            ),
            time = time,
            apiKey = apiCode
        ).map { result ->
            result.map {
                it.key.extractCryptoTicker() to it.value.toAssetPrice()
            }.first()
        }.map {
            check(it.first == crypto)
            it.second
        }

    /** Get the current price in a given fiat, for a set of crypto currencies */
    fun getCurrentPrices(
        cryptoTickerList: Set<String>,
        fiat: String
    ): Single<Map<String, AssetPrice>> =
        api.getCurrentPrices(
            pairs = cryptoTickerList.map { ticker ->
                PriceRequestPairDto(
                    crypto = ticker,
                    fiat = fiat
                )
            },
            apiKey = apiCode
        ).map { result ->
            result.map {
                it.key.extractCryptoTicker() to it.value.toAssetPrice()
            }.toMap()
        }

    /** Get a series of historical price index which covers a range of time represents in specific scale */
    fun getHistoricPriceSince(
        crypto: String,
        fiat: String,
        start: Long, // Epoch seconds
        scale: PriceTimescale
    ): Single<List<AssetPrice>> =
        api.getHistoricPriceSince(
            crypto = crypto,
            fiat = fiat,
            start = start,
            scale = scale.intervalSeconds,
            apiKey = apiCode
        ).map { list ->
            list.filterNot { it.price == null }.map { it.toAssetPrice() }
        }

    private fun String.extractCryptoTicker(): String = substringBefore("-")
}

private fun PriceSymbolDto.toAssetSymbol(): AssetSymbol =
    AssetSymbol(
        ticker = ticker,
        name = name,
        precisionDp = precisionDp,
        isFiat = isFiat
    )

private fun AssetPriceDto.toAssetPrice(): AssetPrice {
    checkNotNull(price)

    return AssetPrice(
        price = price,
        timestamp = timestamp
    )
}