package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import info.blockchain.balance.AssetInfo
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyPaymentDetails
import com.blockchain.nabu.models.data.RecurringBuyState
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class AssetDetailsIntent : MviIntent<AssetDetailsState>

class ShowAssetActionsIntent(
    val account: BlockchainAccount
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState = oldState
}

class AccountActionsLoaded(
    private val account: BlockchainAccount,
    private val actions: AvailableActions
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            selectedAccount = account,
            errorState = AssetDetailsError.NONE,
            actions = actions,
            assetDetailsCurrentStep = AssetDetailsStep.ASSET_ACTIONS
        ).updateBackstack(oldState)
}

class LoadAsset(
    val asset: CryptoAsset
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            asset = asset,
            assetDisplayMap = mapOf()
        )
}

class UpdateTimeSpan(
    val updatedTimeSpan: HistoricalTimeSpan
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(timeSpan = updatedTimeSpan)
}

class HandleActionIntent(
    private val action: AssetAction
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(hostAction = action)
}

object SelectAccount : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            assetDetailsCurrentStep = AssetDetailsStep.SELECT_ACCOUNT
        ).updateBackstack(oldState)
}

object ChartLoading : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(chartLoading = true)
}

class AssetExchangeRateLoaded(
    val exchangeRate: String
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(assetFiatPrice = exchangeRate)
}

class AssetDisplayDetailsLoaded(
    private val assetDisplayMap: AssetDisplayMap
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(assetDisplayMap = assetDisplayMap)
}

class ChartDataLoaded(
    private val chartData: HistoricalRateList
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            chartData = chartData,
            chartLoading = false
        )
}

object ChartDataLoadFailed : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            chartData = emptyList(),
            chartLoading = false,
            errorState = AssetDetailsError.NO_CHART_DATA
        )
}

object AssetDisplayDetailsFailed : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            errorState = AssetDetailsError.NO_ASSET_DETAILS
        )
}

object AssetExchangeRateFailed : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            errorState = AssetDetailsError.NO_EXCHANGE_RATE
        )
}

class RecurringBuyDataLoaded(private val items: Map<String, RecurringBuy>) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            recurringBuys = items
        )
}

object RecurringBuyDataFailed : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            errorState = AssetDetailsError.NO_RECURRING_BUYS
        )
}

object ShowCustodyIntroSheetIntent : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(assetDetailsCurrentStep = AssetDetailsStep.CUSTODY_INTRO_SHEET)
}

object ShowAssetDetailsIntent : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(assetDetailsCurrentStep = AssetDetailsStep.ASSET_DETAILS).updateBackstack(oldState)
}

object CustodialSheetFinished : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(assetDetailsCurrentStep = AssetDetailsStep.ASSET_DETAILS)
}

object ClearSheetDataIntent : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState = AssetDetailsState()
}

object TransactionInFlight : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(errorState = AssetDetailsError.TX_IN_FLIGHT)
}

object ClearActionStates : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            errorState = AssetDetailsError.NONE,
            hostAction = null
        )
}

object ClearSelectedRecurringBuy : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            errorState = AssetDetailsError.NONE,
            selectedRecurringBuy = null
        )
}

object DeleteRecurringBuy : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState = oldState
}

object GetPaymentDetails : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState = oldState
}

class UpdatePaymentDetails(
    val recurringBuyPaymentDetails: RecurringBuyPaymentDetails
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            selectedRecurringBuy = oldState.selectedRecurringBuy?.copy(paymentDetails = recurringBuyPaymentDetails)
        )
}

object ShowInterestDashboard : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(navigateToInterestDashboard = true)
}

class ShowRelevantAssetDetailsSheet(
    val asset: AssetInfo
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState = oldState.updateBackstack(oldState)
}

class ShowRecurringBuySheet(private val recurringBuy: RecurringBuy) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            assetDetailsCurrentStep = AssetDetailsStep.RECURRING_BUY_DETAILS,
            selectedRecurringBuy = recurringBuy
        ).updateBackstack(oldState)
}

object UpdateRecurringBuy : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            selectedRecurringBuy = oldState.selectedRecurringBuy?.copy(state = RecurringBuyState.INACTIVE)
        )
}

object UpdateRecurringBuyError : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            errorState = AssetDetailsError.RECURRING_BUY_DELETE
        )
}

object ReturnToPreviousStep : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState {
        val stack = oldState.stepsBackStack
        require(stack.isNotEmpty())
        val previousStep = stack.pop()
        return oldState.copy(
            stepsBackStack = stack,
            assetDetailsCurrentStep = previousStep,
            hostAction = null,
            errorState = AssetDetailsError.NONE
        )
    }
}

fun AssetDetailsState.updateBackstack(oldState: AssetDetailsState) =
    if (oldState.assetDetailsCurrentStep != this.assetDetailsCurrentStep &&
        oldState.assetDetailsCurrentStep.addToBackStack
    ) {
        val updatedStack = oldState.stepsBackStack
        updatedStack.push(oldState.assetDetailsCurrentStep)

        this.copy(stepsBackStack = updatedStack)
    } else {
        this
    }
