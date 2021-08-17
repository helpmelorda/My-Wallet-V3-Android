package piuk.blockchain.android.ui.interest

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemInterestDashboardAssetInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import timber.log.Timber

class InterestDashboardAssetItem<in T>(
    private val assetResources: AssetResources,
    private val disposable: CompositeDisposable,
    private val interestBalance: InterestBalanceDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val itemClicked: (AssetInfo, Boolean) -> Unit
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as InterestDashboardItem
        return item is InterestAssetInfoItem
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        InterestAssetItemViewHolder(
            ItemInterestDashboardAssetInfoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as InterestAssetItemViewHolder).bind(
        assetResources,
        items[position] as InterestAssetInfoItem,
        disposable,
        interestBalance,
        custodialWalletManager,
        itemClicked
    )
}

private class InterestAssetItemViewHolder(
    private val binding: ItemInterestDashboardAssetInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        assetResources: AssetResources,
        item: InterestAssetInfoItem,
        disposables: CompositeDisposable,
        interestBalance: InterestBalanceDataManager,
        custodialWalletManager: CustodialWalletManager,
        itemClicked: (AssetInfo, Boolean) -> Unit
    ) {
        with(binding) {
            assetResources.loadAssetIcon(itemInterestAssetIcon, item.asset)
            itemInterestAssetTitle.text = item.asset.name

            itemInterestAccBalanceTitle.text =
                context.getString(R.string.interest_dashboard_item_balance_title, item.asset.ticker)
        }

        disposables += Single.zip(
            interestBalance.getBalanceForAsset(item.asset).singleOrError(),
            custodialWalletManager.getInterestAccountRates(item.asset),
            custodialWalletManager.getInterestEligibilityForAsset(item.asset)
        ) { details, rate, eligibility ->
            InterestDetails(
                totalInterest = details.totalInterest,
                balance = details.totalBalance,
                interestRate = rate,
                available = eligibility.eligible,
                disabledReason = eligibility.ineligibilityReason
            )
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { details ->
                    showInterestDetails(details, item, itemClicked)
                },
                onError = {
                    Timber.e("Error loading interest dashboard item: $it")
                    showDisabledState()
                }
            )
    }

    private fun showDisabledState() {
        with(binding) {
            itemInterestCta.isEnabled = false
            itemInterestCta.text = context.getString(R.string.interest_dashboard_item_action_earn)
            itemInterestExplainer.visible()
            itemInterestExplainer.text = context.getString(R.string.interest_item_issue_other)
        }
    }

    private fun showInterestDetails(
        details: InterestDetails,
        item: InterestAssetInfoItem,
        itemClicked: (AssetInfo, Boolean) -> Unit
    ) {
        with(binding) {
            itemInterestAccEarnedLabel.text = details.totalInterest.toStringWithSymbol()

            itemInterestAccBalanceLabel.text = details.balance.toStringWithSymbol()

            setDisabledExplanation(details)

            setCta(item, details, itemClicked)

            setInterestInfo(details, item)
        }
    }

    private fun ItemInterestDashboardAssetInfoBinding.setCta(
        item: InterestAssetInfoItem,
        details: InterestDetails,
        itemClicked: (AssetInfo, Boolean) -> Unit
    ) {
        itemInterestCta.isEnabled = item.isKycGold && details.available
        itemInterestCta.text = if (details.balance.isPositive) {
            context.getString(R.string.interest_dashboard_item_action_view)
        } else {
            context.getString(R.string.interest_dashboard_item_action_earn)
        }

        itemInterestCta.setOnClickListener {
            itemClicked(item.asset, details.balance.isPositive)
        }
    }

    private fun ItemInterestDashboardAssetInfoBinding.setDisabledExplanation(details: InterestDetails) {
        itemInterestExplainer.text = context.getString(
            when (details.disabledReason) {
                IneligibilityReason.REGION -> R.string.interest_item_issue_region
                IneligibilityReason.KYC_TIER -> R.string.interest_item_issue_kyc
                IneligibilityReason.NONE -> R.string.empty
                else -> R.string.interest_item_issue_other
            }
        )

        itemInterestExplainer.visibleIf { details.disabledReason != IneligibilityReason.NONE }
    }

    private fun ItemInterestDashboardAssetInfoBinding.setInterestInfo(
        details: InterestDetails,
        item: InterestAssetInfoItem
    ) {
        val rateIntro = context.getString(R.string.interest_dashboard_item_rate_1)
        val rateInfo = "${details.interestRate}%"
        val rateOutro = context.getString(R.string.interest_dashboard_item_rate_2, item.asset.ticker)

        val sb = SpannableStringBuilder()
            .append(rateIntro)
            .append(rateInfo)
            .append(rateOutro)
        sb.setSpan(StyleSpan(Typeface.BOLD), rateIntro.length,
            rateIntro.length + rateInfo.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        itemInterestInfoText.setText(sb, TextView.BufferType.SPANNABLE)
    }

    private data class InterestDetails(
        val balance: CryptoValue,
        val totalInterest: CryptoValue,
        val interestRate: Double,
        val available: Boolean,
        val disabledReason: IneligibilityReason
    )
}