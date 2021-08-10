package piuk.blockchain.android.ui.interest

import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.disposables.CompositeDisposable
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.resources.AssetResources

class InterestDashboardAdapter(
    assetResources: AssetResources,
    disposables: CompositeDisposable,
    interestBalance: InterestBalanceDataManager,
    custodialWalletManager: CustodialWalletManager,
    verificationClicked: () -> Unit,
    itemClicked: (AssetInfo, Boolean) -> Unit
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(
                InterestDashboardAssetItem(
                    assetResources = assetResources,
                    disposable = disposables,
                    interestBalance = interestBalance,
                    custodialWalletManager = custodialWalletManager,
                    itemClicked = itemClicked
                )
            )
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