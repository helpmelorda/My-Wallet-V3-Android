package piuk.blockchain.android.ui.activity.detail

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.InterestState
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.RecurringBuyFailureReason
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import java.util.Date

interface Copyable {
    val filed: String
}

sealed class ActivityDetailsType
data class Created(val date: Date) : ActivityDetailsType()
data class NextPayment(val date: Date) : ActivityDetailsType()
data class Amount(val value: Money) : ActivityDetailsType()
data class Fee(val feeValue: Money?) : ActivityDetailsType()
data class NetworkFee(val feeValue: Money) : ActivityDetailsType()
data class Value(val currentFiatValue: Money?) : ActivityDetailsType()
data class HistoricValue(
    val fiatAtExecution: Money?,
    val transactionType: TransactionSummary.TransactionType
) : ActivityDetailsType()

data class From(val fromAddress: String?) : ActivityDetailsType()
data class FeeForTransaction(
    val transactionType: TransactionSummary.TransactionType,
    val cryptoValue: Money
) : ActivityDetailsType()

data class To(val toAddress: String?) : ActivityDetailsType()
data class Description(val description: String? = null) : ActivityDetailsType()
data class Action(val action: String = "") : ActivityDetailsType()
data class BuyFee(val feeValue: FiatValue) : ActivityDetailsType()
data class BuyPurchaseAmount(val fundedFiat: FiatValue) : ActivityDetailsType()
data class TotalCostAmount(val fundedFiat: FiatValue) : ActivityDetailsType()
data class FeeAmount(val fundedFiat: FiatValue) : ActivityDetailsType()
data class SellPurchaseAmount(val value: Money) : ActivityDetailsType()
data class TransactionId(val txId: String) : ActivityDetailsType(), Copyable {
    override val filed: String
        get() = txId
}

data class BuyCryptoWallet(val crypto: AssetInfo) : ActivityDetailsType()
data class RecurringBuyFrequency(val frequency: RecurringBuyFrequency, val nextPayment: Date) : ActivityDetailsType()
data class SellCryptoWallet(val currency: String) : ActivityDetailsType()
data class BuyPaymentMethod(val paymentDetails: PaymentDetails) : ActivityDetailsType()
data class SwapReceiveAmount(val receivedAmount: Money) : ActivityDetailsType()
data class XlmMemo(val memo: String) : ActivityDetailsType()

data class PaymentDetails(
    val paymentMethodId: String,
    val label: String? = null,
    val endDigits: String? = null,
    val accountType: String? = null
)

enum class DescriptionState {
    NOT_SET,
    UPDATE_SUCCESS,
    UPDATE_ERROR
}

data class ActivityDetailState(
    val interestState: InterestState? = null,
    val transactionType: TransactionSummary.TransactionType? = null,
    val amount: Money? = null,
    val isPending: Boolean = false,
    val isPendingExecution: Boolean = false,
    val isFeeTransaction: Boolean = false,
    val confirmations: Int = 0,
    val totalConfirmations: Int? = null,
    val listOfItems: Set<ActivityDetailsType> = emptySet(),
    val isError: Boolean = false,
    val hasDeleteError: Boolean = false,
    val recurringBuyState: RecurringBuyState = RecurringBuyState.UNINITIALISED,
    val transactionRecurringBuyState: OrderState = OrderState.UNINITIALISED,
    val recurringBuyError: RecurringBuyFailureReason? = null,
    val descriptionState: DescriptionState = DescriptionState.NOT_SET,
    val recurringBuyId: String? = "",
    val recurringBuyPaymentMethodType: PaymentMethodType? = null,
    val recurringBuyOriginCurrency: String? = null
) : MviState

class ActivityDetailsModel(
    initialState: ActivityDetailState,
    uiScheduler: Scheduler,
    private val interactor: ActivityDetailsInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<ActivityDetailState, ActivityDetailsIntents>(
    initialState, uiScheduler, environmentConfig, crashLogger
) {
    override fun performAction(
        previousState: ActivityDetailState,
        intent: ActivityDetailsIntents
    ): Disposable? {
        return when (intent) {
            is LoadActivityDetailsIntent -> {
                when (intent.activityType) {
                    CryptoActivityType.NON_CUSTODIAL -> loadNonCustodialActivityDetails(intent)
                    CryptoActivityType.CUSTODIAL_TRADING -> loadCustodialTradingActivityDetails(intent)
                    CryptoActivityType.CUSTODIAL_INTEREST -> loadCustodialInterestActivityDetails(intent)
                    CryptoActivityType.CUSTODIAL_TRANSFER -> loadCustodialTransferActivityDetails(intent)
                    CryptoActivityType.SWAP -> loadSwapActivityDetails(intent)
                    CryptoActivityType.SELL -> loadSellActivityDetails(intent)
                    CryptoActivityType.RECURRING_BUY -> loadRecurringBuyTransactionDetails(intent)
                    CryptoActivityType.UNKNOWN -> {
                        throw IllegalStateException(
                            "Cannot load activity details for an unknown account type"
                        )
                    }
                }
                null
            }

            is UpdateDescriptionIntent ->
                interactor.updateItemDescription(
                    intent.txId, intent.asset,
                    intent.description
                ).subscribeBy(
                    onComplete = {
                        process(DescriptionUpdatedIntent)
                    },
                    onError = {
                        process(DescriptionUpdateFailedIntent)
                    }
                )
            is LoadNonCustodialCreationDateIntent -> {
                val activityDate =
                    interactor.loadCreationDate(intent.summaryItem)
                activityDate?.let {
                    process(CreationDateLoadedIntent(activityDate))

                    val nonCustodialActivitySummaryItem = intent.summaryItem
                    loadListDetailsForDirection(nonCustodialActivitySummaryItem)
                } ?: process(CreationDateLoadFailedIntent)
                null
            }
            is DescriptionUpdatedIntent,
            is DescriptionUpdateFailedIntent,
            is ListItemsFailedToLoadIntent,
            is ListItemsLoadedIntent,
            is CreationDateLoadedIntent,
            is CreationDateLoadFailedIntent,
            is ActivityDetailsLoadFailedIntent,
            is LoadCustodialTradingHeaderDataIntent,
            is LoadCustodialInterestHeaderDataIntent,
            is LoadSwapHeaderDataIntent,
            is LoadSellHeaderDataIntent,
            is LoadRecurringBuyDetailsHeaderDataIntent,
            is RecurringBuyDeleteError,
            is RecurringBuyDeletedSuccessfully,
            is LoadNonCustodialHeaderDataIntent,
            is LoadCustodialSendHeaderDataIntent -> null
            is DeleteRecurringBuy -> deleteRecurringBuy(previousState.recurringBuyId.orEmpty())
        }
    }

    private fun loadSellActivityDetails(intent: LoadActivityDetailsIntent) {
        interactor.getTradeActivityDetails(
            asset = intent.asset,
            txHash = intent.txHash
        )?.let {
            process(LoadSellHeaderDataIntent(it))
            interactor.loadSellItems(it).subscribeBy(
                onSuccess = { items ->
                    process(ListItemsLoadedIntent(items))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                })
        } ?: process(ActivityDetailsLoadFailedIntent)
    }

    private fun loadListDetailsForDirection(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
    ) {
        val direction = nonCustodialActivitySummaryItem.transactionType
        when {
            nonCustodialActivitySummaryItem.isFeeTransaction ->
                loadFeeTransactionItems(nonCustodialActivitySummaryItem)
            direction == TransactionSummary.TransactionType.TRANSFERRED ->
                loadTransferItems(nonCustodialActivitySummaryItem)
            direction == TransactionSummary.TransactionType.RECEIVED ->
                loadReceivedItems(nonCustodialActivitySummaryItem)
            direction == TransactionSummary.TransactionType.SENT -> {
                loadSentItems(nonCustodialActivitySummaryItem)
            }
            else -> {
                // do nothing BUY & SELL are a custodial transaction & SWAP has its own activity
            }
        }
    }

    private fun deleteRecurringBuy(id: String) =
        interactor.deleteRecurringBuy(id)
            .subscribeBy(
                onComplete = {
                    process(RecurringBuyDeletedSuccessfully)
                },
                onError = {
                    process(RecurringBuyDeleteError)
                }
            )

    private fun loadNonCustodialActivityDetails(intent: LoadActivityDetailsIntent) =
        interactor.getNonCustodialActivityDetails(
            asset = intent.asset,
            txHash = intent.txHash
        )?.let {
            process(LoadNonCustodialCreationDateIntent(it))
            process(LoadNonCustodialHeaderDataIntent(it))
        } ?: process(ActivityDetailsLoadFailedIntent)

    private fun loadCustodialTradingActivityDetails(intent: LoadActivityDetailsIntent) =
        interactor.getCustodialTradingActivityDetails(
            asset = intent.asset,
            txHash = intent.txHash
        )?.let {
            process(LoadCustodialTradingHeaderDataIntent(it))
            interactor.loadCustodialTradingItems(it).subscribeBy(
                onSuccess = { activityList ->
                    process(ListItemsLoadedIntent(activityList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                })
        } ?: process(ActivityDetailsLoadFailedIntent)

    private fun loadRecurringBuyTransactionDetails(intent: LoadActivityDetailsIntent) =
        interactor.getRecurringBuyTransactionCacheDetails(
            txHash = intent.txHash
        )?.let { cacheTransaction ->
            check(cacheTransaction.recurringBuyId != null) { "No recurring buy id for transaction" }
            process(LoadRecurringBuyDetailsHeaderDataIntent(cacheTransaction))
            interactor.loadRecurringBuysById(cacheTransaction.recurringBuyId)
                .map { cacheTransaction to it }
                .flatMap { (cacheTransaction, recurringBuy) ->
                    interactor.loadRecurringBuyItems(cacheTransaction, recurringBuy)
                }.subscribeBy(
                    onSuccess = { activityList ->
                        process(ListItemsLoadedIntent(activityList))
                    },
                    onError = {
                        process(ListItemsFailedToLoadIntent)
                    })
        } ?: process(ActivityDetailsLoadFailedIntent)

    private fun loadCustodialInterestActivityDetails(intent: LoadActivityDetailsIntent) =
        interactor.getCustodialInterestActivityDetails(
            asset = intent.asset,
            txHash = intent.txHash
        )?.let {
            process(LoadCustodialInterestHeaderDataIntent(it))
            interactor.loadCustodialInterestItems(it).subscribeBy(
                onSuccess = { activityList ->
                    process(ListItemsLoadedIntent(activityList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                })
        } ?: process(ActivityDetailsLoadFailedIntent)

    private fun loadCustodialTransferActivityDetails(intent: LoadActivityDetailsIntent) =
        interactor.getCustodialTransferActivityDetails(
            asset = intent.asset,
            txHash = intent.txHash
        )?.let {
            process(LoadCustodialSendHeaderDataIntent(it))
            interactor.loadCustodialTransferItems(it).subscribeBy(
                onSuccess = { activityList ->
                    process(ListItemsLoadedIntent(activityList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                })
        } ?: process(ActivityDetailsLoadFailedIntent)

    private fun loadSwapActivityDetails(intent: LoadActivityDetailsIntent) =
        interactor.getTradeActivityDetails(
            asset = intent.asset,
            txHash = intent.txHash
        )?.let {
            process(LoadSwapHeaderDataIntent(it))
            interactor.loadSwapItems(it).subscribeBy(
                onSuccess = { swapItems ->
                    process(ListItemsLoadedIntent(swapItems))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                })
        } ?: process(ActivityDetailsLoadFailedIntent)

    private fun loadFeeTransactionItems(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
    ) =
        interactor.loadFeeItems(nonCustodialActivitySummaryItem)
            .subscribeBy(
                onSuccess = { activityItemList ->
                    process(ListItemsLoadedIntent(activityItemList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                }
            )

    private fun loadReceivedItems(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
    ) =
        interactor.loadReceivedItems(nonCustodialActivitySummaryItem)
            .subscribeBy(
                onSuccess = { activityItemList ->
                    process(ListItemsLoadedIntent(activityItemList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                }
            )

    private fun loadTransferItems(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
    ) =
        interactor.loadTransferItems(nonCustodialActivitySummaryItem)
            .subscribeBy(
                onSuccess = { activityItemList ->
                    process(ListItemsLoadedIntent(activityItemList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                }
            )

    private fun loadSentItems(nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem) =
        if (nonCustodialActivitySummaryItem.isConfirmed) {
            interactor.loadConfirmedSentItems(
                nonCustodialActivitySummaryItem
            ).subscribeBy(
                onSuccess = { activityItemsList ->
                    process(ListItemsLoadedIntent(activityItemsList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                }
            )
        } else {
            interactor.loadUnconfirmedSentItems(
                nonCustodialActivitySummaryItem
            ).subscribeBy(
                onSuccess = { activityItemsList ->
                    process(ListItemsLoadedIntent(activityItemsList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                })
        }
}