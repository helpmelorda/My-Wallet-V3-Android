package info.blockchain.balance

import java.math.BigDecimal

interface ExchangeRates {
    fun getLastPrice(cryptoAsset: AssetInfo, fiatName: String): BigDecimal
    fun getLastPriceOfFiat(targetFiat: String, sourceFiat: String): BigDecimal
}