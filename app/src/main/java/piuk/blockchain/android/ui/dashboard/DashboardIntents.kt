package piuk.blockchain.android.ui.dashboard

import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.nabu.models.data.LinkBankTransfer
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import java.math.BigInteger

sealed class DashboardIntent : MviIntent<DashboardState>

class FiatBalanceUpdate(
    private val fiatAssetList: List<FiatBalanceInfo>
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(
            fiatAssets = FiatAssetState(fiatAssetList)
        )
    }
}

class UpdateDashboardCurrencies(
    private val assetList: List<AssetInfo>
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
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

object GetAvailableAssets : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(
            assets = AssetMap(mapOf())
        )
    }
}

object ResetDashboardNavigation : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = null
        )
}

object RefreshAllIntent : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(assets = oldState.assets.reset(), fiatAssets = FiatAssetState())
    }
}

class BalanceUpdate(
    val asset: AssetInfo,
    private val newBalance: Money
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val balance = newBalance as CryptoValue
        require(asset == balance.currency) {
            throw IllegalStateException("CryptoCurrency mismatch")
        }

        val oldAsset = oldState[asset]
        val newAsset = oldAsset.copy(balance = newBalance, hasBalanceError = false)
        val newAssets = oldState.assets.copy(patchAsset = newAsset)

        return oldState.copy(assets = newAssets)
    }
}

class BalanceUpdateError(
    val asset: AssetInfo
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val oldAsset = oldState[asset]
        val newAsset = oldAsset.copy(
            balance = CryptoValue(asset, BigInteger.ZERO),
            hasBalanceError = true
        )
        val newAssets = oldState.assets.copy(patchAsset = newAsset)

        return oldState.copy(assets = newAssets)
    }
}

class CheckForCustodialBalanceIntent(
    val asset: AssetInfo
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
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
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
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
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState = oldState
}

class PriceUpdate(
    val asset: AssetInfo,
    private val latestPrice: ExchangeRate,
    private val prices24HrWithDelta: Prices24HrWithDelta
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val oldAsset = oldState.assets[asset]
        val newAsset = updateAsset(oldAsset, latestPrice, prices24HrWithDelta)

        return oldState.copy(assets = oldState.assets.copy(patchAsset = newAsset))
    }

    private fun updateAsset(
        old: CryptoAssetState,
        latestPrice: ExchangeRate,
        prices24HrWithDelta: Prices24HrWithDelta
    ): CryptoAssetState {
        return old.copy(
            price = latestPrice,
            prices24HrWithDelta = prices24HrWithDelta
        )
    }
}

class PriceHistoryUpdate(
    val asset: AssetInfo,
    private val historicPrices: HistoricalRateList
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
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

class ShowAnnouncement(private val card: AnnouncementCard) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(announcement = card)
    }
}

object ClearAnnouncement : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(announcement = null)
    }
}

class ShowFiatAssetDetails(
    private val fiatAccount: FiatAccount
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.FiatFundsDetails,
            selectedFiatAccount = fiatAccount
        )
}

data class ShowBankLinkingSheet(
    private val fiatAccount: FiatAccount? = null
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.LinkOrDeposit,
            selectedFiatAccount = fiatAccount
        )
}

data class ShowLinkablePaymentMethodsSheet(
    private val paymentMethodsForAction: LinkablePaymentMethodsForAction
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.PaymentMethods,
            linkablePaymentMethodsForAction = paymentMethodsForAction
        )
}

class ShowDashboardSheet(
    private val dashboardNavigationAction: DashboardNavigationAction
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        // Custody sheet isn't displayed via this intent, so filter it out
        oldState.copy(
            dashboardNavigationAction = dashboardNavigationAction,
            activeFlow = null,
            selectedFiatAccount = null
        )
}

class CancelSimpleBuyOrder(
    val orderId: String
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState = oldState
}

object ClearBottomSheet : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            selectedAsset = null
        )
}

class CheckBackupStatus(
    val account: SingleAccount,
    val action: AssetAction
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState = oldState
}

class ShowBackupSheet(
    private val account: SingleAccount,
    private val action: AssetAction
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.BackUpBeforeSend,
            backupSheetDetails = BackupDetails(account, action)
        )
}

class UpdateSelectedCryptoAccount(
    private val singleAccount: SingleAccount,
    private val asset: AssetInfo
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            selectedCryptoAccount = singleAccount,
            selectedAsset = asset
        )
}

class LaunchSendFlow(
    val fromAccount: SingleAccount,
    val action: AssetAction
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            backupSheetDetails = null
        )
}

class LaunchInterestDepositFlow(
    val toAccount: InterestAccount
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            backupSheetDetails = null
        )
}

class LaunchInterestWithdrawFlow(
    val fromAccount: InterestAccount
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
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
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            backupSheetDetails = null
        )
}

class LaunchAssetDetailsFlow(
    val asset: AssetInfo
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = null,
            backupSheetDetails = null
        )
}

class UpdateLaunchDialogFlow(
    private val flow: DialogFlow
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = null,
            activeFlow = flow,
            backupSheetDetails = null
        )
}

object LongCallStarted : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            hasLongCallInProgress = true
        )
}

object LongCallEnded : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            hasLongCallInProgress = false
        )
}

data class LaunchBankLinkFlow(
    val linkBankTransfer: LinkBankTransfer,
    val assetAction: AssetAction
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.LinkBankWithPartner(linkBankTransfer, assetAction),
            activeFlow = null,
            backupSheetDetails = null
        )
}
