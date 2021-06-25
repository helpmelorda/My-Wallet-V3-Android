package piuk.blockchain.android.remoteconfig

import com.blockchain.logging.CrashLogger
import com.blockchain.remoteconfig.RemoteConfig
import com.google.gson.Gson
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.Single
import piuk.blockchain.android.coincore.AssetOrdering

@Deprecated("Asset ordering will be based on balance")
class AssetOrderingRemoteConfig(
    private val config: RemoteConfig,
    private val assetCatalogue: AssetCatalogue,
    private val crashLogger: CrashLogger
) : AssetOrdering {
    private val gson = Gson()

    override fun getAssetOrdering(): Single<List<AssetInfo>> =
        config.getRawJson(ORDERING_KEY)
            .map { gson.fromJson(it, ConfigOrderList::class.java) }
            .map { list ->
                list.order.mapNotNull { ticker ->
                    assetCatalogue.fromNetworkTicker(ticker)
                }
            }.doOnError {
                crashLogger.logException(it, "Error loading asset ordering from remote config")
            }
            .onErrorReturn {
                assetCatalogue.supportedCryptoAssets()
            }

    companion object {
        private const val ORDERING_KEY = "dashboard_crypto_asset_order"
    }

    private data class ConfigOrderList(
        val order: List<String>
    )
}