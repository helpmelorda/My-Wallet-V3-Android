package piuk.blockchain.android.coincore.loader

import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import piuk.blockchain.android.coincore.CryptoAsset

// TODO this will change to support both fiat and crypto, when we have a common interface/class for both
interface AssetLoader {
    fun initAndPreload(): Completable
    val loadedAssets: List<CryptoAsset>

    operator fun get(asset: AssetInfo): CryptoAsset
}

internal class AssetLoaderSwitcher(
    private val features: InternalFeatureFlagApi,
    private val staticLoader: StaticAssetLoader,
    private val dynamicLoader: DynamicAssetLoader
) : AssetLoader {

    private val useDynamicLoader: Boolean by lazy {
        features.isFeatureEnabled(GatedFeature.NEW_SPLIT_DASHBOARD)
    }

    override fun initAndPreload(): Completable =
        if (useDynamicLoader) {
            dynamicLoader.initAndPreload()
        } else {
            staticLoader.initAndPreload()
        }

    override val loadedAssets: List<CryptoAsset>
        get() = if (useDynamicLoader) {
            dynamicLoader.loadedAssets
        } else {
            staticLoader.loadedAssets
        }

    override fun get(asset: AssetInfo): CryptoAsset =
        if (useDynamicLoader) {
            dynamicLoader[asset]
        } else {
            staticLoader[asset]
        }
}
