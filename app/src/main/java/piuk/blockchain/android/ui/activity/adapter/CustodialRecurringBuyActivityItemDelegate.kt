package piuk.blockchain.android.ui.activity.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.RecurringBuyFailureReason
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.RecurringBuyActivitySummaryItem
import piuk.blockchain.android.databinding.DialogActivitiesTxItemBinding
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.setAssetIconColoursWithTint
import piuk.blockchain.android.util.setTransactionHasFailed
import java.util.Date

class CustodialRecurringBuyActivityItemDelegate(
    private val onItemClicked: (AssetInfo, String, CryptoActivityType) -> Unit
) : AdapterDelegate<ActivitySummaryItem> {

    override fun isForViewType(items: List<ActivitySummaryItem>, position: Int): Boolean =
        items[position] is RecurringBuyActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CustodialRecurringBuyActivityViewHolder(
            DialogActivitiesTxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<ActivitySummaryItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CustodialRecurringBuyActivityViewHolder).bind(
        items[position] as RecurringBuyActivitySummaryItem,
        onItemClicked
    )
}

private class CustodialRecurringBuyActivityViewHolder(
    val binding: DialogActivitiesTxItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        tx: RecurringBuyActivitySummaryItem,
        onAccountClicked: (AssetInfo, String, CryptoActivityType) -> Unit
    ) {
        val context = binding.root.context
        with(binding) {
            when {
                tx.transactionState.isPending() || tx.transactionState.isFinished() -> {
                    icon.setImageResource(R.drawable.ic_tx_recurring_buy)
                    icon.setAssetIconColoursWithTint(tx.asset)
                }
                else -> icon.setTransactionHasFailed()
            }

            txType.text = context.resources.getString(R.string.tx_title_buy, tx.asset.ticker)
            statusDate.setTxStatus(tx)
            setTextColours(tx.transactionState)

            tx.setFiatAndCryptoText()

            root.setOnClickListener {
                onAccountClicked(tx.asset, tx.txId, CryptoActivityType.RECURRING_BUY)
            }
        }
    }

    private fun setTextColours(transactionState: OrderState) {
        val context = binding.root.context
        with(binding) {
            when {
                transactionState.isFinished() -> {
                    txType.setTextColor(ContextCompat.getColor(context, R.color.black))
                    statusDate.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
                    assetBalanceFiat.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
                    assetBalanceCrypto.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                transactionState.hasFailed() -> {
                    txType.setTextColor(ContextCompat.getColor(context, R.color.black))
                    statusDate.setTextColor(ContextCompat.getColor(context, R.color.red_600))
                    assetBalanceCrypto.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
                    assetBalanceFiat.gone()
                }
                else -> {
                    txType.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                    statusDate.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                    assetBalanceCrypto.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                    assetBalanceFiat.gone()
                }
            }
        }
    }

    private fun RecurringBuyActivitySummaryItem.setFiatAndCryptoText() {
        with(binding) {
            when {
                transactionState.isFinished() -> {
                    assetBalanceFiat.text = fundedFiat.toStringWithSymbol()
                    assetBalanceCrypto.text = value.toStringWithSymbol()
                }
                transactionState.isPending() || transactionState.hasFailed() || transactionState.isCancelled() -> {
                    assetBalanceCrypto.text = fundedFiat.toStringWithSymbol()
                }
            }
        }
    }

    private fun TextView.setTxStatus(tx: RecurringBuyActivitySummaryItem) {
        text = when {
            tx.transactionState.isFinished() -> Date(tx.timeStampMs).toFormattedDate()
            tx.transactionState.isPending() -> context.getString(R.string.recurring_buy_activity_pending)
            tx.transactionState.hasFailed() -> tx.failureReason?.toShortErrorMessage(context)
                ?: RecurringBuyFailureReason.UNKNOWN.toShortErrorMessage(context)
            tx.transactionState.isCancelled() -> context.getString(R.string.activity_state_canceled)
            else -> ""
        }
    }

    private fun RecurringBuyFailureReason.toShortErrorMessage(context: Context): String =
        when (this) {
            RecurringBuyFailureReason.INSUFFICIENT_FUNDS -> context.getString(
                R.string.recurring_buy_insufficient_funds_short_error
            )
            RecurringBuyFailureReason.INTERNAL_SERVER_ERROR,
            RecurringBuyFailureReason.BLOCKED_BENEFICIARY_ID,
            RecurringBuyFailureReason.FAILED_BAD_FILL,
            RecurringBuyFailureReason.UNKNOWN -> context.getString(R.string.recurring_buy_short_error)
        }
}
