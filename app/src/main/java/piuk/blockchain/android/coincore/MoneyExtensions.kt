package piuk.blockchain.android.coincore

import com.blockchain.core.price.ExchangeRates
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.lang.IllegalStateException
import java.math.RoundingMode

fun Money.toUserFiat(exchangeRates: ExchangeRates): Money =
    when (this) {
        is CryptoValue -> exchangeRates.getLastCryptoToUserFiatRate(this.currency).convert(this)
        is FiatValue -> exchangeRates.getLastFiatToUserFiatRate(this.currencyCode).convert(this)
        else -> throw IllegalStateException("Unknown money type")
    }

fun Money.toFiat(targetFiat: String, exchangeRates: ExchangeRates): Money =
    when (this) {
        is CryptoValue -> exchangeRates.getLastCryptoToFiatRate(this.currency, targetFiat).convert(this)
        is FiatValue -> exchangeRates.getLastFiatToFiatRate(this.currencyCode, targetFiat).convert(this)
        else -> throw IllegalStateException("Unknown money type")
    }

fun FiatValue.toCrypto(exchangeRates: ExchangeRates, cryptoCurrency: AssetInfo) =
    if (isZero) {
        CryptoValue.zero(cryptoCurrency)
    } else {
        val rate = exchangeRates.getLastCryptoToFiatRate(cryptoCurrency, this.currencyCode)
        rate.inverse(RoundingMode.HALF_UP, cryptoCurrency.precisionDp).convert(this)
    }