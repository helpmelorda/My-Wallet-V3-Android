package com.blockchain.core.price

import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

enum class HistoricalTimeSpan {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME
}

data class HistoricalRate(
    val timestamp: Long,
    val rate: Double
)

typealias HistoricalRateList = List<HistoricalRate>

interface ExchangeRates {
    fun getLastCryptoToUserFiatRate(sourceCrypto: AssetInfo): ExchangeRate.CryptoToFiat
    fun getLastFiatToUserFiatRate(sourceFiat: String): ExchangeRate.FiatToFiat

    fun getLastCryptoToFiatRate(sourceCrypto: AssetInfo, targetFiat: String): ExchangeRate.CryptoToFiat
    fun getLastFiatToFiatRate(sourceFiat: String, targetFiat: String): ExchangeRate.FiatToFiat

    fun getLastFiatToCryptoRate(sourceFiat: String, targetCrypto: AssetInfo): ExchangeRate.FiatToCrypto
}

interface ExchangeRatesDataManager : ExchangeRates {
    @Deprecated("TEMP : Remove when CC Accounts updated with unified balance calls")
    fun prefetchCache(assetList: List<AssetInfo>, fiatList: List<String>): Completable
    @Deprecated("TEMP : Remove when CC Accounts updated with unified balance calls")
    fun refetchCache(): Completable

    fun cryptoToUserFiatRate(fromAsset: AssetInfo): Observable<ExchangeRate>
    fun fiatToUserFiatRate(fromFiat: String): Observable<ExchangeRate>
    fun fiatToRateFiatRate(fromFiat: String, toFiat: String): Observable<ExchangeRate>

    fun getHistoricRate(fromAsset: AssetInfo, secSinceEpoch: Long): Single<ExchangeRate>

    fun getHistoricPriceSeries(
        asset: AssetInfo,
        span: HistoricalTimeSpan
    ): Single<HistoricalRateList>

    // Specialised call to historic rates for sparkline caching
    fun get24hPriceSeries(
        asset: AssetInfo
    ): Single<HistoricalRateList>

    val fiatAvailableForRates: List<String>
}
