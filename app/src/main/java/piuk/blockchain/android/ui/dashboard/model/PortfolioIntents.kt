package piuk.blockchain.android.ui.dashboard.model

// todo Ideally we want to map this at the coincore layer to some new object, so that the dashboard doesn't have a dependency on core. Since there are a couple of others that are just passed through, though, this can be for later.
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.nabu.models.data.LinkBankTransfer
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import piuk.blockchain.android.coincore.AccountBalance
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails
import piuk.blockchain.android.ui.transactionflow.DialogFlow

sealed class PortfolioIntent : MviIntent<PortfolioState>

class FiatBalanceUpdate(
    private val fiatAssetList: List<FiatBalanceInfo>
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        return oldState.copy(
            fiatAssets = FiatAssetState(fiatAssetList)
        )
    }
}

class UpdatePortfolioCurrencies(
    private val assetList: List<AssetInfo>
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        return oldState.copy(
            assets = AssetMap(
                assetList.associateBy(
                    keySelector = { it },
                    valueTransform = { CryptoAssetState(it) }
                )
            )
        )
    }
}

object GetAvailableAssets : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        return oldState.copy(
            assets = AssetMap(mapOf())
        )
    }
}

object ResetPortfolioNavigation : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = null
        )
}

object RefreshAllIntent : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        return oldState.copy(assets = oldState.assets.reset(), fiatAssets = FiatAssetState())
    }
}

class BalanceUpdate(
    val asset: AssetInfo,
    private val newBalance: AccountBalance
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        val balance = newBalance.total as CryptoValue
        require(asset == balance.currency) {
            throw IllegalStateException("CryptoCurrency mismatch")
        }

        val oldAsset = oldState[asset]
        val newAsset = oldAsset.copy(accountBalance = newBalance, hasBalanceError = false)
        val newAssets = oldState.assets.copy(patchAsset = newAsset)

        return oldState.copy(assets = newAssets)
    }
}

class BalanceUpdateError(
    val asset: AssetInfo
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        val oldAsset = oldState[asset]
        val newAsset = oldAsset.copy(
            accountBalance = AccountBalance.zero(asset),
            hasBalanceError = true
        )
        val newAssets = oldState.assets.copy(patchAsset = newAsset)

        return oldState.copy(assets = newAssets)
    }
}

class CheckForCustodialBalanceIntent(
    val asset: AssetInfo
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        val oldAsset = oldState[asset]
        val newAsset = oldAsset.copy(
            hasCustodialBalance = false
        )
        val newAssets = oldState.assets.copy(patchAsset = newAsset)
        return oldState.copy(assets = newAssets)
    }
}

class UpdateHasCustodialBalanceIntent(
    val asset: AssetInfo,
    private val hasCustodial: Boolean
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        val oldAsset = oldState[asset]
        val newAsset = oldAsset.copy(
            hasCustodialBalance = hasCustodial
        )
        val newAssets = oldState.assets.copy(patchAsset = newAsset)
        return oldState.copy(assets = newAssets)
    }
}

class RefreshPrices(
    val asset: AssetInfo
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState = oldState
}

class PriceUpdate(
    val asset: AssetInfo,
    private val prices24HrWithDelta: Prices24HrWithDelta
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        val oldAsset = oldState.assets[asset]
        val newAsset = updateAsset(oldAsset, prices24HrWithDelta)

        return oldState.copy(assets = oldState.assets.copy(patchAsset = newAsset))
    }

    private fun updateAsset(
        old: CryptoAssetState,
        prices24HrWithDelta: Prices24HrWithDelta
    ): CryptoAssetState {
        return old.copy(
            accountBalance = old.accountBalance?.copy(
                exchangeRate = prices24HrWithDelta.currentRate
            ),
            prices24HrWithDelta = prices24HrWithDelta
        )
    }
}

class PriceHistoryUpdate(
    val asset: AssetInfo,
    private val historicPrices: HistoricalRateList
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        val oldAsset = oldState.assets[asset]
        val newAsset = updateAsset(oldAsset, historicPrices)

        return oldState.copy(assets = oldState.assets.copy(patchAsset = newAsset))
    }

    private fun updateAsset(
        old: CryptoAssetState,
        historicPrices: HistoricalRateList
    ): CryptoAssetState {
        val trend = historicPrices.map { it.rate.toFloat() }
        return old.copy(priceTrend = trend)
    }
}

class ShowAnnouncement(private val card: AnnouncementCard) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        return oldState.copy(announcement = card)
    }
}

object ClearAnnouncement : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState {
        return oldState.copy(announcement = null)
    }
}

class ShowFiatAssetDetails(
    private val fiatAccount: FiatAccount
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.FiatFundsDetails,
            selectedFiatAccount = fiatAccount
        )
}

data class ShowBankLinkingSheet(
    private val fiatAccount: FiatAccount? = null
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.LinkOrDeposit,
            selectedFiatAccount = fiatAccount
        )
}

data class ShowLinkablePaymentMethodsSheet(
    private val paymentMethodsForAction: LinkablePaymentMethodsForAction
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.PaymentMethods,
            linkablePaymentMethodsForAction = paymentMethodsForAction
        )
}

class ShowPortfolioSheet(
    private val dashboardNavigationAction: DashboardNavigationAction
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        // Custody sheet isn't displayed via this intent, so filter it out
        oldState.copy(
            dashboardNavigationAction = dashboardNavigationAction,
            activeFlow = null,
            selectedFiatAccount = null
        )
}

class CancelSimpleBuyOrder(
    val orderId: String
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState = oldState
}

object ClearBottomSheet : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            selectedAsset = null
        )
}

class CheckBackupStatus(
    val account: SingleAccount,
    val action: AssetAction
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState = oldState
}

class ShowBackupSheet(
    private val account: SingleAccount,
    private val action: AssetAction
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.BackUpBeforeSend,
            backupSheetDetails = BackupDetails(account, action)
        )
}

class UpdateSelectedCryptoAccount(
    private val singleAccount: SingleAccount,
    private val asset: AssetInfo
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            selectedCryptoAccount = singleAccount,
            selectedAsset = asset
        )
}

class LaunchSendFlow(
    val fromAccount: SingleAccount,
    val action: AssetAction
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            backupSheetDetails = null
        )
}

class LaunchInterestDepositFlow(
    val toAccount: InterestAccount
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            backupSheetDetails = null
        )
}

class LaunchInterestWithdrawFlow(
    val fromAccount: InterestAccount
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            backupSheetDetails = null
        )
}

class LaunchBankTransferFlow(
    val account: SingleAccount,
    val action: AssetAction,
    val shouldLaunchBankLinkTransfer: Boolean
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            backupSheetDetails = null
        )
}

class LaunchAssetDetailsFlow(
    val asset: AssetInfo
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            backupSheetDetails = null
        )
}

class UpdateLaunchDialogFlow(
    private val flow: DialogFlow
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = flow,
            backupSheetDetails = null
        )
}

object LongCallStarted : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            hasLongCallInProgress = true
        )
}

object LongCallEnded : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            hasLongCallInProgress = false
        )
}

data class LaunchBankLinkFlow(
    val linkBankTransfer: LinkBankTransfer,
    val assetAction: AssetAction
) : PortfolioIntent() {
    override fun reduce(oldState: PortfolioState): PortfolioState =
        oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.LinkBankWithPartner(
                linkBankTransfer, assetAction
            ),
            activeFlow = null,
            backupSheetDetails = null
        )
}
