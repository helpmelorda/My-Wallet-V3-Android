package piuk.blockchain.android.coincore

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.nabu.datamanagers.InterestState
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.RecurringBuyFailureReason
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.helperfunctions.JavaHashCode
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import kotlin.math.sign

abstract class CryptoActivitySummaryItem : ActivitySummaryItem() {
    abstract val asset: AssetInfo
}

class FiatActivitySummaryItem(
    val currency: String,
    override val exchangeRates: ExchangeRatesDataManager,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: FiatAccount,
    val type: TransactionType,
    val state: TransactionState
) : ActivitySummaryItem() {
    override fun toString(): String = "currency = $currency " +
        "transactionType  = $type " +
        "timeStamp  = $timeStampMs " +
        "total  = ${value.toStringWithSymbol()} " +
        "txId (hash)  = $txId "
}

abstract class ActivitySummaryItem : Comparable<ActivitySummaryItem> {
    protected abstract val exchangeRates: ExchangeRatesDataManager

    abstract val txId: String
    abstract val timeStampMs: Long

    abstract val value: Money

    fun fiatValue(selectedFiat: String): Money =
        value.toFiat(selectedFiat, exchangeRates)

    final override operator fun compareTo(
        other: ActivitySummaryItem
    ) = (other.timeStampMs - timeStampMs).sign

    abstract val account: SingleAccount
}

data class TradeActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val txId: String,
    override val timeStampMs: Long,
    val sendingValue: Money,
    val sendingAccount: SingleAccount,
    val sendingAddress: String?,
    val receivingAddress: String?,
    val state: CustodialOrderState,
    val direction: TransferDirection,
    val receivingValue: Money,
    val depositNetworkFee: Single<Money>,
    val withdrawalNetworkFee: Money,
    val currencyPair: CurrencyPair,
    val fiatValue: FiatValue,
    val fiatCurrency: String
) : ActivitySummaryItem() {
    override val account: SingleAccount
        get() = sendingAccount

    override val value: Money
        get() = sendingValue
}

data class RecurringBuyActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val asset: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: SingleAccount,
    val fundedFiat: FiatValue,
    val transactionState: OrderState,
    val failureReason: RecurringBuyFailureReason?,
    val fee: FiatValue,
    val paymentMethodId: String,
    val paymentMethodType: PaymentMethodType,
    val type: OrderType,
    val recurringBuyId: String?
) : CryptoActivitySummaryItem()

data class CustodialInterestActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val asset: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: CryptoAccount,
    val status: InterestState,
    val type: TransactionSummary.TransactionType,
    val confirmations: Int,
    val accountRef: String,
    val recipientAddress: String
) : CryptoActivitySummaryItem() {
    fun isPending(): Boolean =
        status == InterestState.PENDING ||
            status == InterestState.PROCESSING ||
            status == InterestState.MANUAL_REVIEW
}

data class CustodialTradingActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val asset: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: CryptoAccount,
    val fundedFiat: FiatValue,
    val status: OrderState,
    val type: OrderType,
    val fee: FiatValue,
    val paymentMethodId: String,
    val paymentMethodType: PaymentMethodType,
    val depositPaymentId: String,
    val recurringBuyId: String? = null
) : CryptoActivitySummaryItem()

data class CustodialTransferActivitySummaryItem(
    override val asset: AssetInfo,
    override val exchangeRates: ExchangeRatesDataManager,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: SingleAccount,
    val fee: Money,
    val recipientAddress: String,
    val txHash: String,
    val state: TransactionState,
    val fiatValue: FiatValue,
    val type: TransactionType
) : CryptoActivitySummaryItem() {
    val isConfirmed: Boolean by lazy {
        state == TransactionState.COMPLETED
    }
}

abstract class NonCustodialActivitySummaryItem : CryptoActivitySummaryItem() {

    abstract val transactionType: TransactionSummary.TransactionType
    abstract val fee: Observable<CryptoValue>

    abstract val inputsMap: Map<String, CryptoValue>

    abstract val outputsMap: Map<String, CryptoValue>

    abstract val description: String?

    open val confirmations = 0
    open val doubleSpend: Boolean = false
    open val isFeeTransaction = false
    open val isPending: Boolean = false
    open var note: String? = null

    override fun toString(): String = "cryptoCurrency = $asset" +
        "transactionType  = $transactionType " +
        "timeStamp  = $timeStampMs " +
        "total  = ${value.toStringWithSymbol()} " +
        "txId (hash)  = $txId " +
        "inputsMap  = $inputsMap " +
        "outputsMap  = $outputsMap " +
        "confirmations  = $confirmations " +
        "doubleSpend  = $doubleSpend " +
        "isPending  = $isPending " +
        "note = $note"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as NonCustodialActivitySummaryItem?

        return this.asset == that?.asset &&
            this.transactionType == that.transactionType &&
            this.timeStampMs == that.timeStampMs &&
            this.value == that.value &&
            this.txId == that.txId &&
            this.inputsMap == that.inputsMap &&
            this.outputsMap == that.outputsMap &&
            this.confirmations == that.confirmations &&
            this.doubleSpend == that.doubleSpend &&
            this.isFeeTransaction == that.isFeeTransaction &&
            this.isPending == that.isPending &&
            this.note == that.note
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + asset.hashCode()
        result = 31 * result + transactionType.hashCode()
        result = 31 * result + JavaHashCode.hashCode(timeStampMs)
        result = 31 * result + value.hashCode()
        result = 31 * result + txId.hashCode()
        result = 31 * result + inputsMap.hashCode()
        result = 31 * result + outputsMap.hashCode()
        result = 31 * result + JavaHashCode.hashCode(confirmations)
        result = 31 * result + JavaHashCode.hashCode(isFeeTransaction)
        result = 31 * result + JavaHashCode.hashCode(doubleSpend)
        result = 31 * result + (note?.hashCode() ?: 0)
        return result
    }

    open fun updateDescription(description: String): Completable =
        Completable.error(IllegalStateException("Update description not supported"))

    val isConfirmed: Boolean by unsafeLazy {
        confirmations >= asset.requiredConfirmations
    }
}

typealias ActivitySummaryList = List<ActivitySummaryItem>
