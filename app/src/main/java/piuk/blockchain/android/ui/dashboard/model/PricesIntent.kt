package piuk.blockchain.android.ui.dashboard.model

import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class PricesIntent : MviIntent<PricesState> {

    internal object GetAvailableAssets : PricesIntent() {
        override fun reduce(oldState: PricesState): PricesState {
            return oldState.copy(
                availablePrices = emptyList()
            )
        }
    }

    internal object RefreshAllPrices : PricesIntent() {
        override fun reduce(oldState: PricesState): PricesState {
            return oldState.copy()
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

    internal object ClearBottomSheet : PricesIntent() {
        override fun reduce(oldState: PricesState): PricesState =
            oldState.copy(
                activeFlow = null,
                selectedAsset = null
            )
    }
}