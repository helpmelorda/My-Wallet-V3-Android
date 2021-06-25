package piuk.blockchain.android.coincore

import info.blockchain.balance.AssetInfo
import io.reactivex.Single

@Deprecated("Asset ordering will be based on balance")
interface AssetOrdering {
    fun getAssetOrdering(): Single<List<AssetInfo>>
}
