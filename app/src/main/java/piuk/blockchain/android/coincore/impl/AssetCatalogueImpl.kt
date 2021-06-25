package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import java.util.Locale

class AssetCatalogueImpl : AssetCatalogue {
    // Brute force impl for now, but this will operate from a downloaded cache ultimately
    override fun fromNetworkTicker(symbol: String): AssetInfo? =
        when (symbol.toUpperCase(Locale.ROOT)) {
            CryptoCurrency.BTC.ticker -> CryptoCurrency.BTC
            CryptoCurrency.ETHER.ticker -> CryptoCurrency.ETHER
            CryptoCurrency.BCH.ticker -> CryptoCurrency.BCH
            CryptoCurrency.XLM.ticker -> CryptoCurrency.XLM
            CryptoCurrency.ALGO.ticker -> CryptoCurrency.ALGO
            DGLD.ticker -> DGLD
            PAX.ticker -> PAX
            USDT.ticker -> USDT
            AAVE.ticker -> AAVE
            YFI.ticker -> YFI
            CryptoCurrency.DOT.ticker -> CryptoCurrency.DOT
            else -> null
        }

    override fun supportedCryptoAssets(): List<AssetInfo> =
        listOf(
            CryptoCurrency.BTC,
            CryptoCurrency.ETHER,
            CryptoCurrency.BCH,
            CryptoCurrency.XLM,
            CryptoCurrency.ALGO,
            DGLD,
            PAX,
            USDT,
            AAVE,
            YFI,
            CryptoCurrency.DOT
        )

    override fun supportedL2Assets(chain: AssetInfo): List<AssetInfo> =
        supportedCryptoAssets().filter { it.l2chain == chain }

    override fun supportedCustodialAssets(): List<AssetInfo> =
        listOf(
            CryptoCurrency.BTC,
            CryptoCurrency.ETHER,
            CryptoCurrency.BCH,
            CryptoCurrency.XLM,
            CryptoCurrency.ALGO,
            DGLD,
            PAX,
            USDT,
            AAVE,
            YFI,
            CryptoCurrency.DOT
        )
}
