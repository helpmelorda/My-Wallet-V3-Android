package piuk.blockchain.android.ui.interest

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.AssetInfo
import io.reactivex.disposables.CompositeDisposable
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.resources.AssetResources

class InterestDashboardAdapter(
    assetResources: AssetResources,
    disposables: CompositeDisposable,
    custodialWalletManager: CustodialWalletManager,
    verificationClicked: () -> Unit,
    itemClicked: (AssetInfo, Boolean) -> Unit
) :
    DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(
                InterestDashboardAssetItem(
                    assetResources, disposables, custodialWalletManager, itemClicked
                ))
            addAdapterDelegate(InterestDashboardVerificationItem(verificationClicked))
        }
    }
}

sealed class InterestDashboardItem

object InterestIdentityVerificationItem : InterestDashboardItem()
class InterestAssetInfoItem(
    val isKycGold: Boolean,
    val asset: AssetInfo
) : InterestDashboardItem()