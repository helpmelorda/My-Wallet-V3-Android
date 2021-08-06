package info.blockchain.wallet.prices

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.ExchangeRate
import io.reactivex.rxjava3.core.Observable

/**
 * Access to streams of indicative rates.
 * These are suitable for on the fly converting for information purposes.
 */
interface IndicativeFiatPriceService {

    /**
     * A stream of indicative rates from [AssetInfo] to Fiat.
     * These are suitable for converting Crypto to Fiat for display purposes.
     */
    fun indicativeRateStream(from: AssetInfo, toFiat: String): Observable<ExchangeRate.CryptoToFiat>

    /**
     * A stream of indicative rates from Fiat to [AssetInfo].
     * These are suitable for converting Fiat to Crypto for display purposes.
     */
    fun indicativeRateStream(fromFiat: String, to: AssetInfo): Observable<ExchangeRate.FiatToCrypto> =
        indicativeRateStream(to, fromFiat).map { it.inverse() }
}
