package piuk.blockchain.android.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.disposables.CompositeDisposable
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.data.activity.historicRate.HistoricRateFetcher
import piuk.blockchain.android.databinding.DialogActivitiesTxItemBinding
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.getResolvedColor
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.setAssetIconColoursWithTint
import piuk.blockchain.android.util.setTransactionIsConfirming
import java.util.Date

class CustodialSendActivityItemDelegate(
    private val prefs: CurrencyPrefs,
    private val historicRateFetcher: HistoricRateFetcher,
    private val onItemClicked: (AssetInfo, String, CryptoActivityType) -> Unit // crypto, txID, type
) : AdapterDelegate<ActivitySummaryItem> {

    override fun isForViewType(items: List<ActivitySummaryItem>, position: Int): Boolean =
        items[position] is CustodialTransferActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CustodialTradeActivityItemViewHolder(
            DialogActivitiesTxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<ActivitySummaryItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CustodialTradeActivityItemViewHolder).bind(
        items[position] as CustodialTransferActivitySummaryItem,
        prefs.selectedFiatCurrency,
        historicRateFetcher,
        onItemClicked
    )
}

private class CustodialTradeActivityItemViewHolder(
    private val binding: DialogActivitiesTxItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun bind(
        tx: CustodialTransferActivitySummaryItem,
        selectedFiatCurrency: String,
        historicRateFetcher: HistoricRateFetcher,
        onAccountClicked: (AssetInfo, String, CryptoActivityType) -> Unit
    ) {
        disposables.clear()
        with(binding) {
            if (tx.isConfirmed) {
                icon.setTransactionDirection(tx)
                icon.setAssetIconColoursWithTint(tx.asset)
            } else {
                icon.setTransactionIsConfirming()
            }

            setTextColours(tx.isConfirmed)

            statusDate.text = Date(tx.timeStampMs).toFormattedDate()

            txType.setDirectionText(tx)

            assetBalanceFiat.gone()
            assetBalanceCrypto.text = tx.value.toStringWithSymbol()
            assetBalanceFiat.bindAndConvertFiatBalance(tx, disposables, selectedFiatCurrency, historicRateFetcher)

            binding.root.setOnClickListener {
                onAccountClicked(
                    tx.asset, tx.txId, CryptoActivityType.CUSTODIAL_TRANSFER
                )
            }
        }
    }

    private fun TextView.setDirectionText(tx: CustodialTransferActivitySummaryItem) =
        when (tx.type) {
            TransactionType.DEPOSIT -> text = context.getString(
                R.string.tx_title_receive, tx.asset.ticker
            )
            TransactionType.WITHDRAWAL -> text = context.getString(
                R.string.tx_title_send, tx.asset.ticker
            )
        }

    private fun ImageView.setTransactionDirection(tx: CustodialTransferActivitySummaryItem) =
        when (tx.type) {
            TransactionType.DEPOSIT -> setImageResource(R.drawable.ic_tx_receive)
            TransactionType.WITHDRAWAL -> setImageResource(R.drawable.ic_tx_sent)
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