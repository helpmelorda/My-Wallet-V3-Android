package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.core.price.Prices24HrWithDelta
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsFlow

sealed class PricesIntent : MviIntent<PricesState> {

    internal object GetAvailableAssets : PricesIntent() {
        override fun reduce(oldState: PricesState): PricesState {
            return oldState.copy(
                availablePrices = emptyMap()
            )
        }
    }

    internal class AssetListUpdate(
        private val assetList: List<AssetInfo>
    ) : PricesIntent() {
        override fun reduce(oldState: PricesState): PricesState {
            return oldState.copy(
                availablePrices = assetList.map { it to AssetPriceState(assetInfo = it) }.toMap()
            )
        }
    }

    internal class GetAssetPrice(
        val asset: AssetInfo
    ) : PricesIntent() {
        override fun reduce(oldState: PricesState): PricesState = oldState
    }

    internal class AssetPriceUpdate(
        private val asset: AssetInfo,
        private val price: Prices24HrWithDelta
    ) : PricesIntent() {
        override fun reduce(oldState: PricesState): PricesState {
            val priceState = AssetPriceState(
                assetInfo = asset,
                prices = price
            )

            val map = oldState.availablePrices.toMutableMap()
            map[asset] = priceState
            return oldState.copy(availablePrices = map)
        }
    }

    internal class LaunchAssetDetailsFlow(
        val asset: AssetInfo
    ) : PricesIntent() {
        override fun reduce(oldState: PricesState): PricesState =
            oldState.copy(
                activeFlow = null
            )
    }

    internal class UpdateLaunchDetailsFlow(
        private val flow: AssetDetailsFlow
    ) : PricesIntent() {
        override fun reduce(oldState: PricesState): PricesState =
            oldState.copy(
                activeFlow = flow
            )
    }

    internal object ClearBottomSheet : PricesIntent() {
        override fun reduce(oldState: PricesState): PricesState =
            oldState.copy(
                activeFlow = null,
                selectedAsset = null
            )
    }
}