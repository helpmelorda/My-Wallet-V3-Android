package piuk.blockchain.android.ui.activity.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import com.blockchain.utils.toFormattedDate
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.disposables.CompositeDisposable
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import com.blockchain.data.activity.historicRate.HistoricRateFetcher
import piuk.blockchain.android.databinding.DialogActivitiesTxItemBinding
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.getResolvedColor
import piuk.blockchain.android.util.setAssetIconColoursWithTint
import piuk.blockchain.android.util.gone
import java.util.Date

class NonCustodialActivityItemDelegate<in T>(
    private val currencyPrefs: CurrencyPrefs,
    private val historicRateFetcher: HistoricRateFetcher,
    private val onItemClicked: (AssetInfo, String, CryptoActivityType) -> Unit // crypto, txID, type
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is NonCustodialActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        NonCustodialActivityItemViewHolder(
            DialogActivitiesTxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as NonCustodialActivityItemViewHolder).bind(
        items[position] as NonCustodialActivitySummaryItem,
        currencyPrefs.selectedFiatCurrency,
        historicRateFetcher,
        onItemClicked
    )
}

private class NonCustodialActivityItemViewHolder(
    private val binding: DialogActivitiesTxItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun bind(
        tx: NonCustodialActivitySummaryItem,
        selectedFiatCurrency: String,
        historicRateFetcher: HistoricRateFetcher,
        onAccountClicked: (AssetInfo, String, CryptoActivityType) -> Unit
    ) {
        disposables.clear()
        with(binding) {
            if (tx.isConfirmed) {
                icon.setTransactionTypeIcon(tx.transactionType, tx.isFeeTransaction)
                icon.setAssetIconColoursWithTint(tx.asset)
            } else {
                icon.setIsConfirming()
            }

            statusDate.text = Date(tx.timeStampMs).toFormattedDate()

            txType.setTxLabel(tx.asset, tx.transactionType, tx.isFeeTransaction)

            setTextColours(tx.isConfirmed)

            assetBalanceFiat.gone()
            assetBalanceCrypto.text = tx.value.toStringWithSymbol()
            assetBalanceFiat.bindAndConvertFiatBalance(tx, disposables, selectedFiatCurrency, historicRateFetcher)

            txRoot.setOnClickListener { onAccountClicked(tx.asset, tx.txId, CryptoActivityType.NON_CUSTODIAL) }
        }
    }

    private fun setTextColours(isConfirmed: Boolean) {
        with(binding) {
            if (isConfirmed) {
                txType.setTextColor(context.getResolvedColor(R.color.black))
                statusDate.setTextColor(context.getResolvedColor(R.color.grey_600))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_600))
                assetBalanceCrypto.setTextColor(context.getResolvedColor(R.color.black))
            } else {
                txType.setTextColor(context.getResolvedColor(R.color.grey_400))
                statusDate.setTextColor(context.getResolvedColor(R.color.grey_400))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_400))
                assetBalanceCrypto.setTextColor(context.getResolvedColor(R.color.grey_400))
            }
        }
    }
}

private fun ImageView.setTransactionTypeIcon(
    transactionType: TransactionSummary.TransactionType,
    isFeeTransaction: Boolean
) {
    setImageResource(
        if (isFeeTransaction) {
            R.drawable.ic_tx_sent
        } else {
            when (transactionType) {
                TransactionSummary.TransactionType.TRANSFERRED -> R.drawable.ic_tx_transfer
                TransactionSummary.TransactionType.RECEIVED -> R.drawable.ic_tx_receive
                TransactionSummary.TransactionType.SENT -> R.drawable.ic_tx_sent
                TransactionSummary.TransactionType.BUY -> R.drawable.ic_tx_buy
                TransactionSummary.TransactionType.SELL -> R.drawable.ic_tx_sell
                TransactionSummary.TransactionType.SWAP -> R.drawable.ic_tx_swap
                else -> R.drawable.ic_tx_buy
            }
        }
    )
}

private fun ImageView.setIsConfirming() =
    apply {
        setImageDrawable(
            AppCompatResources.getDrawable(
                context,
                R.drawable.ic_tx_confirming
            )
        )
        background = null
        setColorFilter(Color.TRANSPARENT)
    }

private fun TextView.setTxLabel(
    asset: AssetInfo,
    transactionType: TransactionSummary.TransactionType,
    isFeeTransaction: Boolean
) {
    val resId = if (isFeeTransaction) {
        R.string.tx_title_fee
    } else {
        when (transactionType) {
            TransactionSummary.TransactionType.TRANSFERRED -> R.string.tx_title_transfer
            TransactionSummary.TransactionType.RECEIVED -> R.string.tx_title_receive
            TransactionSummary.TransactionType.SENT -> R.string.tx_title_send
            TransactionSummary.TransactionType.BUY -> R.string.tx_title_buy
            TransactionSummary.TransactionType.SELL -> R.string.tx_title_sell
            TransactionSummary.TransactionType.SWAP -> R.string.tx_title_swap
            else -> R.string.empty
        }
    }

    text = context.resources.getString(resId, asset.ticker)
}
