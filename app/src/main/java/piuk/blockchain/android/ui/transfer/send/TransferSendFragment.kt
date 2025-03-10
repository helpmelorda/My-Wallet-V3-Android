package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.View
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.LaunchOrigin
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.simplebuy.BuySellClicked
import piuk.blockchain.android.simplebuy.BuySellType
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.ui.customviews.account.DefaultCellDecorator
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionLauncher
import piuk.blockchain.android.ui.transactionflow.analytics.SendAnalyticsEvent
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalyticsAccountType
import piuk.blockchain.android.ui.transfer.AccountSelectorFragment
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent

class TransferSendFragment : AccountSelectorFragment(), DialogFlow.FlowHost {

    private val analytics: Analytics by inject()
    private val txLauncher: TransactionLauncher by inject()

    override val fragmentAction: AssetAction
        get() = AssetAction.Send

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderList()
    }

    private fun renderList() {
        setEmptyStateDetails(
            R.string.transfer_wallets_empty_title,
            R.string.transfer_wallets_empty_details,
            R.string.transfer_wallet_buy_crypto
        ) {
            analytics.logEvent(TransferAnalyticsEvent.NoBalanceCtaClicked)
            analytics.logEvent(BuySellClicked(origin = LaunchOrigin.SEND, type = BuySellType.BUY))
            (activity as? HomeNavigator)?.launchBuySell()
        }

        initialiseAccountSelectorWithHeader(
            statusDecorator = ::statusDecorator,
            onAccountSelected = ::doOnAccountSelected,
            title = R.string.transfer_send_crypto_title,
            label = R.string.transfer_send_crypto_label,
            icon = R.drawable.ic_send_blue_circle
        )
    }

    private fun statusDecorator(account: BlockchainAccount): CellDecorator =
        if (account is CryptoAccount) {
            SendCellDecorator(account)
        } else {
            DefaultCellDecorator()
        }

    private fun doOnAccountSelected(account: BlockchainAccount) {
        require(account is CryptoAccount)

        analytics.logEvent(TransferAnalyticsEvent.SourceWalletSelected(account))
        analytics.logEvent(
            SendAnalyticsEvent.SendSourceAccountSelected(
                currency = account.asset.ticker,
                fromAccountType = TxFlowAnalyticsAccountType.fromAccount(
                    account
                )
            )
        )
        startTransactionFlow(account)
    }

    private fun startTransactionFlow(fromAccount: CryptoAccount) {
        txLauncher.startFlow(
            activity = requireActivity(),
            sourceAccount = fromAccount,
            action = AssetAction.Send,
            fragmentManager = childFragmentManager,
            flowHost = this@TransferSendFragment
        )
    }

    override fun doOnEmptyList() {
        super.doOnEmptyList()
        analytics.logEvent(TransferAnalyticsEvent.NoBalanceViewDisplayed)
    }

    override fun onFlowFinished() {
        refreshItems(showLoader = false)
    }

    companion object {
        fun newInstance() = TransferSendFragment()
    }
}
