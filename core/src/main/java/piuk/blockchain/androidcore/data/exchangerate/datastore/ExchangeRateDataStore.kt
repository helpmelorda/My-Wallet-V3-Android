package piuk.blockchain.androidcore.data.exchangerate.datastore

import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber
import java.lang.IllegalStateException
import java.math.BigDecimal

// TODO: This will need rewriting before we greatly expand the number of supported cryptos.
// updateExchangeRates() will have to be removed and we'll have to access individual
// prices or demand, rather than batch loading on system start.
// Also, this caching via system prefs is ugly and will also need replacing with something
// more elegant. This is out of scope for this current refactor pass, however.
class ExchangeRateDataStore(
    private val exchangeRateService: ExchangeRateService,
    private val assetCatalogue: AssetCatalogue,
    private val prefs: PersistentPrefs
) {
    // Map assets to a map of FIAT -> CURRENT PRICE in that fiat
    private val tickerCache: MutableMap<AssetInfo, Map<String, PriceDatum>> = HashMap()

    fun updateExchangeRates(): Completable = Single.merge(
        assetCatalogue.supportedCryptoAssets()
            .map { asset ->
                exchangeRateService.getExchangeRateMap(asset)
                    .doOnSuccess { tickerCache.put(asset, it.toMap()) }
            }
        ).ignoreElements()

    // TODO: The gets the 'known' list of supported fiat - for prices.
    // Def a hack and should be fixed
    fun getCurrencyLabels(): Array<String> =
        tickerCache.entries.first().value.keys.toTypedArray()

    fun getLastPrice(asset: AssetInfo, fiatCurrency: String): Double {
        if (fiatCurrency.isEmpty()) {
            throw IllegalArgumentException("No currency supplied")
        }

        val tickerData = asset.tickerData()

        val prefsKey = "LAST_KNOWN_${asset.ticker}_VALUE_FOR_CURRENCY_$fiatCurrency"

        val lastKnown = try {
            prefs.getValue(prefsKey, "0.0").toDouble()
        } catch (e: NumberFormatException) {
            Timber.e(e)
            prefs.setValue(prefsKey, "0.0")
            0.0
        }

        val price = tickerData[fiatCurrency]?.price?.also {
            prefs.setValue(prefsKey, it.toString())
        } ?: lastKnown

        // adding this requirement ensures the app won't crash due to divide by 0
        // link to issue: https://tinyurl.com/rmf6um4h
        require(price != 0.0)

        return price
    }

    fun getFiatLastPrice(targetFiat: String, sourceFiat: String): Double {
        val targetCurrencyPrice =
            CryptoCurrency.BTC.tickerData()[targetFiat]?.price ?: return 0.0
        val sourceCurrencyPrice =
            CryptoCurrency.BTC.tickerData()[sourceFiat]?.price ?: return 0.0
        return targetCurrencyPrice.div(sourceCurrencyPrice)
    }

    private fun AssetInfo.tickerData() =
        tickerCache[this] ?: throw IllegalStateException("No data available for asset ${this.ticker}")

    fun getHistoricPrice(
        asset: AssetInfo,
        fiat: String,
        timeInSeconds: Long
    ): Single<BigDecimal> =
        exchangeRateService.getHistoricPrice(asset, fiat, timeInSeconds)
            .map { it.toBigDecimal() }
}
