package piuk.blockchain.android.simplebuy

import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.base.FlowFragment
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

interface SimpleBuyScreen : SlidingModalBottomDialog.Host, FlowFragment {
    fun navigator(): SimpleBuyNavigator

    override fun onSheetClosed() {}
}

interface SimpleBuyNavigator : SlidingModalBottomDialog.Host, SmallSimpleBuyNavigator {
    fun goToBuyCryptoScreen(
        addToBackStack: Boolean = true,
        preselectedAsset: AssetInfo,
        preselectedPaymentMethodId: String?
    )
    fun goToCheckOutScreen(addToBackStack: Boolean = true)
    fun goToKycVerificationScreen(addToBackStack: Boolean = true)
    fun goToPendingOrderScreen()
    fun startKyc()
    fun pop()
    fun hasMoreThanOneFragmentInTheStack(): Boolean
    fun goToPaymentScreen(addToBackStack: Boolean = true, isPaymentAuthorised: Boolean = false)
    fun launchBankAuthWithError(errorState: ErrorState)
    fun goToSetupFirstRecurringBuy(addToBackStack: Boolean = true)
    fun goToFirstRecurringBuyCreated(addToBackStack: Boolean = true)
}

interface SmallSimpleBuyNavigator {
    fun exitSimpleBuyFlow()
}