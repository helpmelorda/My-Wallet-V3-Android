package piuk.blockchain.android.ui.dashboard.model

import androidx.annotation.VisibleForTesting
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.models.data.LinkBankTransfer
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.isErc20
import info.blockchain.balance.percentageDelta
import info.blockchain.balance.total
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinComponent
import piuk.blockchain.android.coincore.AccountBalance
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails
import piuk.blockchain.android.ui.settings.LinkablePaymentMethods
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.io.Serializable

class AssetMap(private val map: Map<AssetInfo, CryptoAssetState>) :
    Map<AssetInfo, CryptoAssetState> by map {
    override operator fun get(key: AssetInfo): CryptoAssetState {
        return map.getOrElse(key) {
            throw IllegalArgumentException("$key is not a known CryptoCurrency")
        }
    }

    // TODO: This is horrendously inefficient. Fix it!
    fun copy(): AssetMap {
        val assets = toMutableMap()
        return AssetMap(assets)
    }

    fun copy(patchBalance: AccountBalance): AssetMap {
        val assets = toMutableMap()
        // CURRENCY HERE
        val balance = patchBalance.total as CryptoValue
        val value = get(balance.currency).copy(accountBalance = patchBalance)
        assets[balance.currency] = value
        return AssetMap(assets)
    }

    fun copy(patchAsset: CryptoAssetState): AssetMap {
        val assets = toMutableMap()
        assets[patchAsset.currency] = patchAsset
        return AssetMap(assets)
    }

    fun reset(): AssetMap {
        val assets = toMutableMap()
        map.values.forEach { assets[it.currency] = it.reset() }
        return AssetMap(assets)
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun mapOfAssets(vararg pairs: Pair<AssetInfo, CryptoAssetState>) = AssetMap(mapOf(*pairs))

interface DashboardItem

interface BalanceState : DashboardItem {
    val isLoading: Boolean
    val fiatBalance: Money?
    val delta: Pair<Money, Double>?
    operator fun get(currency: AssetInfo): CryptoAssetState
    val assetList: List<CryptoAssetState>
    fun getFundsFiat(fiat: String): Money
}

data class FiatBalanceInfo(
    val balance: Money,
    val userFiat: Money,
    val account: FiatAccount
)

data class FiatAssetState(
    val fiatAccounts: List<FiatBalanceInfo> = emptyList()
) : DashboardItem {

    val totalBalance: Money? =
        if (fiatAccounts.isEmpty()) {
            null
        } else {
            fiatAccounts.map {
                it.userFiat
            }.total()
        }
}

sealed class DashboardNavigationAction {
    object StxAirdropComplete : DashboardNavigationAction()
    object BackUpBeforeSend : DashboardNavigationAction()
    object SimpleBuyCancelOrder : DashboardNavigationAction()
    object FiatFundsDetails : DashboardNavigationAction()
    object LinkOrDeposit : DashboardNavigationAction()
    object FiatFundsNoKyc : DashboardNavigationAction()
    object InterestSummary : DashboardNavigationAction()
    object PaymentMethods : DashboardNavigationAction()
    class LinkBankWithPartner(
        override val linkBankTransfer: LinkBankTransfer,
        override val assetAction: AssetAction
    ) : DashboardNavigationAction(), LinkBankNavigationAction

    fun isBottomSheet() =
        this !is LinkBankNavigationAction
}

interface LinkBankNavigationAction {
    val linkBankTransfer: LinkBankTransfer
    val assetAction: AssetAction
}

data class PortfolioState(
    val assets: AssetMap = AssetMap(emptyMap()),
    val dashboardNavigationAction: DashboardNavigationAction? = null,
    val activeFlow: DialogFlow? = null,
    val announcement: AnnouncementCard? = null,
    val fiatAssets: FiatAssetState? = null,
    val selectedFiatAccount: FiatAccount? = null,
    val selectedCryptoAccount: SingleAccount? = null,
    val selectedAsset: AssetInfo? = null,
    val backupSheetDetails: BackupDetails? = null,
    val linkablePaymentMethodsForAction: LinkablePaymentMethodsForAction? = null,
    val hasLongCallInProgress: Boolean = false
) : MviState, BalanceState, KoinComponent {

    // If ALL the assets are refreshing, then report true. Else false
    override val isLoading: Boolean by unsafeLazy {
        assets.values.all { it.isLoading }
    }

    override val fiatBalance: Money? by unsafeLazy {
        addFiatBalance(cryptoAssetFiatBalances())
    }

    private fun cryptoAssetFiatBalances() = assets.values
        .filter { !it.isLoading && it.fiatBalance != null }
        .map { it.fiatBalance!! }
        .ifEmpty { null }?.total()

    private val fiatBalance24h: Money? by unsafeLazy {
        addFiatBalance(cryptoAssetFiatBalances24h())
    }

    private fun cryptoAssetFiatBalances24h() = assets.values
        .filter { !it.isLoading && it.fiatBalance24h != null }
        .map { it.fiatBalance24h!! }
        .ifEmpty { null }?.total()

    private fun addFiatBalance(balance: Money?): Money? {
        val fiatAssetBalance = fiatAssets?.totalBalance

        return if (balance != null) {
            if (fiatAssetBalance != null) {
                balance + fiatAssetBalance
            } else {
                balance
            }
        } else {
            fiatAssetBalance
        }
    }

    override val delta: Pair<Money, Double>? by unsafeLazy {
        val current = fiatBalance
        val old = fiatBalance24h

        if (current != null && old != null) {
            Pair(current - old, current.percentageDelta(old))
        } else {
            null
        }
    }

    override val assetList: List<CryptoAssetState> = assets.values.toList()

    override operator fun get(currency: AssetInfo): CryptoAssetState =
        assets[currency]

    override fun getFundsFiat(fiat: String): Money =
        fiatAssets?.totalBalance ?: FiatValue.zero(fiat)

    val assetMapKeys = assets.keys

    val erc20Assets = assetMapKeys.filter { it.isErc20() }
}

data class CryptoAssetState(
    val currency: AssetInfo,
    val accountBalance: AccountBalance? = null,
    val prices24HrWithDelta: Prices24HrWithDelta? = null,
    val priceTrend: List<Float> = emptyList(),
    val hasBalanceError: Boolean = false,
    val hasCustodialBalance: Boolean = false
) : DashboardItem {
    val fiatBalance: Money? by unsafeLazy {
        accountBalance?.exchangeRate?.let { p -> accountBalance.total.let { p.convert(it) } }
    }

    val fiatBalance24h: Money? by unsafeLazy {
        prices24HrWithDelta?.previousRate?.let { p -> accountBalance?.total?.let { p.convert(it) } }
    }

    val priceDelta: Double by unsafeLazy {
        prices24HrWithDelta?.delta24h ?: Double.NaN
    }

    val isLoading: Boolean by unsafeLazy {
        accountBalance == null || prices24HrWithDelta == null
    }

    fun reset(): CryptoAssetState = CryptoAssetState(currency)
}

sealed class LinkablePaymentMethodsForAction(
    open val linkablePaymentMethods: LinkablePaymentMethods
) : Serializable {
    data class LinkablePaymentMethodsForSettings(
        override val linkablePaymentMethods: LinkablePaymentMethods
    ) : LinkablePaymentMethodsForAction(linkablePaymentMethods)

    data class LinkablePaymentMethodsForDeposit(
        override val linkablePaymentMethods: LinkablePaymentMethods
    ) : LinkablePaymentMethodsForAction(linkablePaymentMethods)

    data class LinkablePaymentMethodsForWithdraw(
        override val linkablePaymentMethods: LinkablePaymentMethods
    ) : LinkablePaymentMethodsForAction(linkablePaymentMethods)
}

class PortfolioModel(
    initialState: PortfolioState,
    mainScheduler: Scheduler,
    private val interactor: PortfolioInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<PortfolioState, PortfolioIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(
        previousState: PortfolioState,
        intent: PortfolioIntent
    ): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is GetAvailableAssets -> {
                interactor.getAvailableAssets(this)
            }
            is RefreshAllIntent -> {
                interactor.refreshBalances(this, AssetFilter.All, previousState)
            }
            is BalanceUpdate -> {
                process(CheckForCustodialBalanceIntent(intent.asset))
                null
            }
            is CheckForCustodialBalanceIntent -> interactor.checkForCustodialBalance(
                this,
                intent.asset
            )
            is UpdateHasCustodialBalanceIntent -> {
                process(RefreshPrices(intent.asset))
                null
            }
            is RefreshPrices -> interactor.refreshPrices(this, intent.asset)
            is PriceUpdate -> interactor.refreshPriceHistory(this, intent.asset)
            is CheckBackupStatus -> checkBackupStatus(intent.account, intent.action)
            is CancelSimpleBuyOrder -> interactor.cancelSimpleBuyOrder(intent.orderId)
            is LaunchAssetDetailsFlow -> interactor.getAssetDetailsFlow(this, intent.asset)
            is LaunchInterestDepositFlow ->
                interactor.getInterestDepositFlow(this, intent.toAccount)
            is LaunchInterestWithdrawFlow ->
                interactor.getInterestWithdrawFlow(this, intent.fromAccount)
            is LaunchBankTransferFlow -> processBankTransferFlow(intent)
            is LaunchSendFlow -> interactor.getSendFlow(this, intent.fromAccount, intent.action)
            is FiatBalanceUpdate,
            is BalanceUpdateError,
            is PriceHistoryUpdate,
            is ClearAnnouncement,
            is ShowAnnouncement,
            is ShowFiatAssetDetails,
            is ShowBankLinkingSheet,
            is ShowPortfolioSheet,
            is UpdateLaunchDialogFlow,
            is ClearBottomSheet,
            is UpdateSelectedCryptoAccount,
            is ShowBackupSheet,
            is UpdatePortfolioCurrencies,
            is LaunchBankLinkFlow,
            is ResetPortfolioNavigation,
            is ShowLinkablePaymentMethodsSheet,
            is LongCallStarted,
            is LongCallEnded -> null
        }
    }

    private fun processBankTransferFlow(intent: LaunchBankTransferFlow) =
        when (intent.action) {
            AssetAction.FiatDeposit -> {
                interactor.getBankDepositFlow(
                    this,
                    intent.account,
                    intent.action,
                    intent.shouldLaunchBankLinkTransfer
                )
            }
            AssetAction.Withdraw -> {
                interactor.getBankWithdrawalFlow(
                    this,
                    intent.account,
                    intent.action,
                    intent.shouldLaunchBankLinkTransfer
                )
            }
            else -> {
                null
            }
        }

    private fun checkBackupStatus(account: SingleAccount, action: AssetAction): Disposable =
        interactor.hasUserBackedUp()
            .subscribeBy(
                onSuccess = { isBackedUp ->
                    if (isBackedUp) {
                        process(LaunchSendFlow(account, action))
                    } else {
                        process(ShowBackupSheet(account, action))
                    }
                }, onError = { Timber.e(it) }
            )

    override fun distinctIntentFilter(
        previousIntent: PortfolioIntent,
        nextIntent: PortfolioIntent
    ): Boolean {
        return when (previousIntent) {
            is UpdateLaunchDialogFlow -> {
                if (nextIntent is ClearBottomSheet) {
                    true
                } else {
                    super.distinctIntentFilter(previousIntent, nextIntent)
                }
            }
            else -> super.distinctIntentFilter(previousIntent, nextIntent)
        }
    }
}
