package piuk.blockchain.android.simplebuy

import androidx.annotation.VisibleForTesting
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.androidcore.utils.extensions.flatMapBy
import timber.log.Timber

// Ensure that the local and remote SimpleBuy state is the same.
// Resolution strategy is:
//  - check simple buy is enabled
//  - inflate the local state, if any
//  - fetch the remote state, if any
//      - if the remote state is the same as the local state, then do nothing
//      - if the remote state exists and the local state is in an earlier stage, use the remote state
//      - if the remote state and the local state refer to the same order (id) and the remote state
//        is completed/error/cancel, then wipe the local state
//

class SimpleBuySyncFactory(
    private val custodialWallet: CustodialWalletManager,
    private val serializer: SimpleBuyPrefsSerializer
) {

    fun performSync(): Completable = syncStates()
        .doOnSuccess { v ->
            Timber.d("SB Sync: Success")
            serializer.update(v)
        }
        .doOnComplete {
            Timber.d("SB Sync: Complete")
            serializer.clear()
        }
        .ignoreElement()
        .observeOn(Schedulers.computation())
        .doOnError {
            Timber.d("SB Sync: FAILED because $it")
        }

    fun currentState(): SimpleBuyState? =
        serializer.fetch().apply {
            Timber.d("SB Sync: state == $this")
        }

    // If we have a local state in awaiting funds, check the server and clear it if the backend has transitioned
    // to any completed state (pending, cancelled, finished, failed)
    fun lightweightSync(): Completable =
        maybeInflateLocalState()
            .flatMapBy(
                onSuccess = { localState ->
                    if (localState.orderState == OrderState.AWAITING_FUNDS) {
                        updateWithRemote(localState)
                            .doOnComplete { serializer.clear() }
                            .doOnSuccess { state -> serializer.update(state) }
                    } else {
                        Maybe.empty()
                    }
                },
                onError = { Maybe.empty() }, // Do nothing
                onComplete = {
                    serializer.clear()
                    Maybe.empty()
                } // No local state. Do nothing
            )
            .ignoreElement()

    private fun syncStates(): Maybe<SimpleBuyState> {
        return maybeInflateLocalState()
            .flatMap { updateWithRemote(it) }
            .flatMapBy(
                onSuccess = { checkForRemoteOverride(it) },
                onError = {
                    Maybe.error(it)
                },
                onComplete = { Maybe.defer { getRemotePendingBuy() } }
            )
    }

    private fun getRemotePendingBuy(): Maybe<SimpleBuyState> {
        return custodialWallet.getAllOutstandingBuyOrders()
            .flatMapMaybe { list ->
                list.sortedByDescending { it.expires }
                    .firstOrNull {
                        it.state == OrderState.AWAITING_FUNDS ||
                            it.state == OrderState.PENDING_EXECUTION ||
                            it.state == OrderState.PENDING_CONFIRMATION
                    }?.toSimpleBuyStateMaybe() ?: Maybe.empty()
            }
    }

    private fun checkForRemoteOverride(localState: SimpleBuyState): Maybe<SimpleBuyState> {
        return if (localState.orderState < OrderState.PENDING_CONFIRMATION) {
            custodialWallet.getAllOutstandingBuyOrders()
                .flatMapMaybe { list ->
                    list.sortedByDescending { it.expires }
                        .firstOrNull {
                            it.state == OrderState.AWAITING_FUNDS ||
                                it.state == OrderState.PENDING_EXECUTION ||
                                it.state == OrderState.PENDING_CONFIRMATION
                        }?.toSimpleBuyStateMaybe() ?: Maybe.just(localState)
                }
        } else {
            Maybe.just(localState)
        }
    }

    private fun BuySellOrder.isDefinedCardPayment() =
        paymentMethodType == PaymentMethodType.PAYMENT_CARD &&
            paymentMethodId != PaymentMethod.UNDEFINED_CARD_PAYMENT_ID

    private fun BuySellOrder.isDefinedBankTransferPayment() =
        paymentMethodType == PaymentMethodType.BANK_TRANSFER &&
            paymentMethodId != PaymentMethod.UNDEFINED_BANK_TRANSFER_PAYMENT_ID

    private fun BuySellOrder.toSimpleBuyStateMaybe(): Maybe<SimpleBuyState> = when {
        isDefinedCardPayment() -> {
            custodialWallet.getCardDetails(paymentMethodId).flatMapMaybe {
                Maybe.just(
                    this.toSimpleBuyState().copy(
                        selectedPaymentMethod = SelectedPaymentMethod(
                            it.cardId,
                            it.partner,
                            it.detailedLabel(),
                            PaymentMethodType.PAYMENT_CARD,
                            true
                        )
                    )
                )
            }
        }
        isDefinedBankTransferPayment() -> {
            custodialWallet.getLinkedBank(paymentMethodId).flatMapMaybe {
                Maybe.just(
                    toSimpleBuyState().copy(
                        selectedPaymentMethod = SelectedPaymentMethod(
                            it.id, null, it.accountName, PaymentMethodType.BANK_TRANSFER, true
                        )
                    )
                )
            }
        }
        else -> {
            Maybe.just(toSimpleBuyState())
        }
    }

    private fun updateWithRemote(localState: SimpleBuyState): Maybe<SimpleBuyState> =
        getRemoteForLocal(localState.id)
            .defaultIfEmpty(localState)
            .map { remoteState ->
                Timber.d("SB Sync: local.state == ${localState.orderState}, remote.state == ${remoteState.orderState}")
                if (localState.orderState < remoteState.orderState) {
                    Timber.d("SB Sync: Take remote")
                    remoteState
                } else {
                    Timber.d("SB Sync: Take local")
                    localState
                }
            }
            .flatMapMaybe { state ->
                when (state.orderState) {
                    OrderState.UNINITIALISED,
                    OrderState.INITIALISED,
                    OrderState.PENDING_EXECUTION,
                    OrderState.PENDING_CONFIRMATION,
                    OrderState.AWAITING_FUNDS -> Maybe.just(state)
                    OrderState.FINISHED,
                    OrderState.CANCELED,
                    OrderState.FAILED,
                    OrderState.UNKNOWN -> Maybe.empty()
                }
            }

    private fun getRemoteForLocal(id: String?): Maybe<SimpleBuyState> =
        id?.let {
            custodialWallet.getBuyOrder(it)
                .map { order -> order.toSimpleBuyState() }
                .toMaybe()
                .onErrorResumeNext { Maybe.empty() }
        } ?: Maybe.empty()

    private fun maybeInflateLocalState(): Maybe<SimpleBuyState> =
        Maybe.fromCallable { serializer.fetch() }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun BuySellOrder.toSimpleBuyState(): SimpleBuyState =
    SimpleBuyState(
        id = id,
        amount = fiat,
        fiatCurrency = fiat.currencyCode,
        selectedCryptoAsset = crypto.currency,
        orderState = state,
        fee = fee,
        orderValue = orderValue as? CryptoValue,
        orderExchangePrice = price,
        selectedPaymentMethod = SelectedPaymentMethod(
            id = paymentMethodId,
            paymentMethodType = paymentMethodType,
            isEligible = true
        ),
        expirationDate = expires,
        currentScreen = configureCurrentScreen(state)
    )

private fun configureCurrentScreen(
    state: OrderState
): FlowScreen =
    if (state == OrderState.PENDING_CONFIRMATION) {
        FlowScreen.ENTER_AMOUNT
    } else {
        FlowScreen.CHECKOUT
    }
