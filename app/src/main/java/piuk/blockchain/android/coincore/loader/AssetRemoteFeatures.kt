package piuk.blockchain.android.coincore.loader

import androidx.annotation.VisibleForTesting
import com.blockchain.remoteconfig.RemoteConfig
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.lang.IllegalArgumentException

private typealias RemoteFeatureMap = Map<String, List<String>>

// Allow dynamic assets to be remotely configured by feature set
// Remote input looks like, eg:
// {
//    "DOT": ["FullSupport"],
//    "ALGO": ["SendReceive"],
//    "DOGE":["Brokerage"],
//    "CLOUT":["CanBuySell"],
//    "LTC":[CanBuy, CanSwap],
//    "ETC":[CanSwap],
//    "USDC":["FullSupport"]
// }

internal enum class RemoteFeature {
    Balance, // Not used by remote
    CanBuy, // Brokerage support - simple buy
    CanSell, // Brokerage support - simple sell
    CanSwap, // Brokerage support - can swap
    CanSend,
    CanReceive,

    // Multiflags:
    FullSupport, // == CanSend & CanReceive & CanBuy & CanSell & CanSwap
    SendReceive, // == CanSend & CanReceive
    Brokerage, // == CanBuy & CanSell & CanSwap
    CanBuySell; // == CanBuy & CanSell

    fun reduceMultiFlag(): Set<RemoteFeature> =
        when (this) {
            FullSupport -> setOf(CanSend, CanReceive, CanBuy, CanSell, CanSwap)
            SendReceive -> setOf(CanSend, CanReceive)
            Brokerage -> setOf(CanBuy, CanSell, CanSwap)
            CanBuySell -> setOf(CanBuy, CanSell)
            else -> setOf(this)
        }

    companion object {
        fun valueOrNull(str: String) =
            try {
                // Upper case the search here, so that case is effectively ignored in the remote input
                val input = str.uppercase()
                values().firstOrNull { it.name.uppercase() == input }
            } catch (t: IllegalArgumentException) {
                null
            }
    }
}

private fun List<String>.toFeatureSet() =
    setOf(RemoteFeature.Balance) +
        this.mapNotNull { RemoteFeature.valueOrNull(it) }
            .toSet()
            .map { it.reduceMultiFlag() }
            .flatten()

internal class AssetRemoteFeatureLookup(
    private val remoteConfig: RemoteConfig
) {
    private lateinit var featureMap: Map<String, Set<RemoteFeature>>

    fun init(staticAssets: Set<AssetInfo>): Completable =
        remoteConfig.getRawJson(DYNAMIC_ASSET_FEATURES)
            .map { json ->
                Json.decodeFromString<RemoteFeatureMap>(json)
            }.doOnSuccess { rawMap ->
                featureMap = buildFeatureMap(
                    staticAssets,
                    rawMap.mapValues { it.value.toFeatureSet() }
                )
            }.doOnError {
                featureMap = buildFeatureMap(
                    staticAssets,
                    defaultFallbackMap
                )
            }.ignoreElement()
            .onErrorComplete()

    private fun buildFeatureMap(
        staticAssets: Set<AssetInfo>,
        dynamicAssets: Map<String, Set<RemoteFeature>>
    ): Map<String, Set<RemoteFeature>> =
        dynamicAssets.toMutableMap().apply {
            staticAssets.forEach { asset ->
                put(asset.ticker, fullSupport)
            }
        }

    fun featuresFor(ticker: AssetInfo) =
        featureMap[ticker.ticker] ?: emptySet()

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
        const val DYNAMIC_ASSET_FEATURES = "custodial_only_tokens"

        private val fullSupport = setOf(
            RemoteFeature.Balance,
            RemoteFeature.CanBuy,
            RemoteFeature.CanSell,
            RemoteFeature.CanSwap,
            RemoteFeature.CanSend,
            RemoteFeature.CanReceive
        )
        private val defaultFallbackMap = mapOf(
            DOT.ticker to fullSupport,
            ALGO.ticker to fullSupport
        )
    }
}
