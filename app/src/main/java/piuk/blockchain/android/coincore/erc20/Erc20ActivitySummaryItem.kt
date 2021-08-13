package piuk.blockchain.android.coincore.erc20

import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.erc20.model.Erc20HistoryEvent
import com.blockchain.core.price.ExchangeRatesDataManager
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.math.BigInteger

internal class Erc20ActivitySummaryItem(
    override val asset: AssetInfo,
    private val event: Erc20HistoryEvent,
    private val accountHash: String,
    private val erc20DataManager: Erc20DataManager,
    override val exchangeRates: ExchangeRatesDataManager,
    lastBlockNumber: BigInteger,
    override val account: CryptoAccount
) : NonCustodialActivitySummaryItem() {

    override val transactionType: TransactionSummary.TransactionType by unsafeLazy {
        when {
            event.isToAccount(accountHash)
                && event.isFromAccount(accountHash) -> TransactionSummary.TransactionType.TRANSFERRED
            event.isFromAccount(accountHash) -> TransactionSummary.TransactionType.SENT
            else -> TransactionSummary.TransactionType.RECEIVED
        }
    }

    override val timeStampMs: Long = event.timestamp * 1000

    override val value: CryptoValue = event.value

    override val description: String?
        get() = erc20DataManager.getErc20TxNote(asset = asset, txHash = txId)

    override val fee: Observable<CryptoValue>
        get() = event.fee.toObservable()

    override val txId: String = event.transactionHash

    override val inputsMap: Map<String, CryptoValue> =
        mapOf(event.from to event.value)

    override val outputsMap: Map<String, CryptoValue> =
        mapOf(event.to to event.value)

    override val confirmations: Int = (lastBlockNumber - event.blockNumber).toInt()

    override fun updateDescription(description: String): Completable =
        erc20DataManager.putErc20TxNote(asset = asset, txHash = txId, note = description)
}
