package piuk.blockchain.android.coincore.impl

import androidx.annotation.VisibleForTesting
import com.blockchain.remoteconfig.RemoteConfig
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.isCustodial
import info.blockchain.balance.isCustodialOnly
import io.reactivex.Completable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Locale

class AssetCatalogueImpl(
    private val remoteConfig: RemoteConfig
) : AssetCatalogue {

    private lateinit var fullAssetLookup: Map<String, AssetInfo>

    fun init(): Completable =
        remoteConfig.getRawJson(CUSTODIAL_ONLY_TOKENS)
            .map { json ->
                Json.decodeFromString<List<String>>(json)
            }.onErrorReturn {
                configDefaultEnabled
            }.map { enabled ->
                buildAssetLookup(enabled.toSet())
            }.ignoreElement()

    private fun buildAssetLookup(enabledDynamicCustodial: Set<String>) {
        val enabledCustodial = enabledDynamicCustodial.mapNotNull { assetTicker ->
            dynamicCustodialOnlyAssets.firstOrNull { it.ticker == assetTicker }
        }

        val enabledAssets = nonDynamicAssets +
            enabledCustodial +
            dynamicErc20Assets

        fullAssetLookup = enabledAssets.associateBy { it.ticker.toUpperCase(Locale.ROOT) }
    }

    // Brute force impl for now, but this will operate from a downloaded cache ultimately
    override fun fromNetworkTicker(symbol: String): AssetInfo? =
        fullAssetLookup[symbol.toUpperCase(Locale.ROOT)]

    override val supportedCryptoAssets: List<AssetInfo> by lazy {
        fullAssetLookup.values.toList()
    }

    override val supportedCustodialAssets: List<AssetInfo> by lazy {
        fullAssetLookup.values.filter { it.isCustodial }
    }

    override fun supportedL2Assets(chain: AssetInfo): List<AssetInfo> =
        supportedCryptoAssets.filter { it.l2chain == chain }

    val dynamicCustodialAssets: List<AssetInfo> by lazy {
        fullAssetLookup.values.filter { it.isCustodialOnly }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
        const val CUSTODIAL_ONLY_TOKENS = "custodial_only_tokens"

        private val nonDynamicAssets: Set<AssetInfo> =
            setOf(
                CryptoCurrency.BTC,
                CryptoCurrency.ETHER,
                CryptoCurrency.BCH,
                CryptoCurrency.XLM
            )

        private val dynamicErc20Assets: Set<AssetInfo> =
            setOf(
                WDGLD,
                PAX,
                USDT,
                AAVE,
                YFI
            )

        private val dynamicCustodialOnlyAssets: Set<AssetInfo> =
            setOf(
                ALGO,
                DOT,
                DOGE,
                CLOUT,
                LTC,
                ETC,
                ZEN,
                XTZ,
                STX,
                MOB,
                THETA,
                NEAR,
                EOS,
                OGN,
                ENJ,
                COMP,
                LINK,
                TBTC,
                WBTC,
                SNX,
                SUSHI,
                ZRX,
                USDC,
                UNI,
                DAI,
                BAT
            )

        // Fallback to current asset set, in case json parsing fails:
        private val configDefaultEnabled = listOf(DOT.ticker, ALGO.ticker)
    }
}
