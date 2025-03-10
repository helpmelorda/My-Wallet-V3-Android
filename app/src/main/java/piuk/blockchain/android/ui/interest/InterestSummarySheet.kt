package piuk.blockchain.android.ui.interest

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRates
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.notifications.analytics.LaunchOrigin
import info.blockchain.balance.AssetInfo
import com.blockchain.utils.secondsToDays
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.toUserFiat
import piuk.blockchain.android.databinding.DialogSheetInterestDetailsBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.transactionflow.analytics.InterestAnalytics
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InterestSummarySheet : SlidingModalBottomDialog<DialogSheetInterestDetailsBinding>() {
    interface Host : SlidingModalBottomDialog.Host {
        fun goToActivityFor(account: BlockchainAccount)
        fun goToInterestDeposit(toAccount: InterestAccount)
        fun goToInterestWithdraw(fromAccount: InterestAccount)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a InterestSummarySheet.Host"
        )
    }

    private lateinit var account: SingleAccount
    private lateinit var asset: AssetInfo

    override fun initBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogSheetInterestDetailsBinding =
        DialogSheetInterestDetailsBinding.inflate(inflater, container, false)

    private val disposables = CompositeDisposable()
    private val interestBalance: InterestBalanceDataManager by scopedInject()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val coincore: Coincore by scopedInject()
    @Suppress("unused")
    private val features: InternalFeatureFlagApi by inject()

    private val listAdapter: InterestSummaryAdapter by lazy { InterestSummaryAdapter() }

    override fun initControls(binding: DialogSheetInterestDetailsBinding) {
        binding.interestDetailsList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
            adapter = listAdapter
        }

        binding.apply {
            interestDetailsTitle.text = account.label
            interestDetailsSheetHeader.text = asset.name
            interestDetailsLabel.text = asset.name

            interestDetailsAssetWithIcon.updateIcon(account as CryptoAccount)

            disposables += coincore.allWalletsWithActions(setOf(AssetAction.InterestDeposit)).map { accounts ->
                accounts.filter { account -> account is CryptoAccount && account.asset == asset }
            }
                .onErrorReturn { emptyList() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { accounts ->
                    if (accounts.isNotEmpty()) {
                        interestDetailsDepositCta.visible()
                        interestDetailsDepositCta.text =
                            getString(R.string.tx_title_deposit, asset.ticker)
                        interestDetailsDepositCta.setOnClickListener {
                            analytics.logEvent(InterestAnalytics.InterestSummaryDepositCta)
                            host.goToInterestDeposit(account as InterestAccount)
                        }
                    } else {
                        interestDetailsDepositCta.gone()
                    }
                }
        }

        disposables += Singles.zip(
            interestBalance.getBalanceForAsset(asset).firstOrError(),
            custodialWalletManager.getInterestLimits(asset),
            custodialWalletManager.getInterestAccountRates(asset)
        ).observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { (details, limits, interestRate) ->
                    compositeToView(
                        CompositeInterestDetails(
                            totalInterest = details.totalInterest,
                            pendingInterest = details.pendingInterest,
                            balance = (details.totalBalance - details.lockedBalance) as CryptoValue,
                            lockupDuration = limits.interestLockUpDuration.secondsToDays(),
                            interestRate = interestRate,
                            nextInterestPayment = limits.nextInterestPayment
                        )
                    )
                },
                onError = {
                    Timber.e("Error loading interest summary details: $it")
                }
            )
    }

    private fun compositeToView(composite: CompositeInterestDetails) {
        with(binding) {
            if (composite.balance.isPositive) {
                interestDetailsWithdrawCta.text =
                    getString(R.string.tx_title_withdraw, asset.ticker)
                interestDetailsWithdrawCta.visible()
                interestDetailsWithdrawCta.setOnClickListener {
                    analytics.logEvent(InterestAnalytics.InterestWithdrawalClicked(
                        currency = composite.balance.currencyCode,
                        origin = LaunchOrigin.SAVINGS_PAGE
                    ))
                    analytics.logEvent(InterestAnalytics.InterestSummaryWithdrawCta)
                    host.goToInterestWithdraw(account as InterestAccount)
                }
            }
        }

        val itemList = mutableListOf<InterestSummaryInfoItem>()
        itemList.apply {
            add(
                InterestSummaryInfoItem(
                    getString(R.string.interest_summary_total),
                    composite.totalInterest.toStringWithSymbol()
                )
            )

            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val formattedDate = sdf.format(composite.nextInterestPayment)
            add(InterestSummaryInfoItem(getString(R.string.interest_summary_next_payment), formattedDate))

            add(
                InterestSummaryInfoItem(
                    getString(R.string.interest_summary_accrued),
                    composite.pendingInterest.toStringWithSymbol()
                )
            )

            add(
                InterestSummaryInfoItem(
                    getString(R.string.interest_summary_hold_period),
                    getString(R.string.interest_summary_hold_period_days, composite.lockupDuration)
                )
            )

            add(InterestSummaryInfoItem(getString(R.string.interest_summary_rate), "${composite.interestRate}%"))
        }

        composite.balance.run {
            binding.apply {
                interestDetailsCryptoValue.text = toStringWithSymbol()
                interestDetailsFiatValue.text = toUserFiat(exchangeRates)
                    .toStringWithSymbol()
            }
        }

        listAdapter.items = itemList
    }

    companion object {
        fun newInstance(
            singleAccount: SingleAccount,
            selectedAsset: AssetInfo
        ): InterestSummarySheet =
            InterestSummarySheet().apply {
                account = singleAccount
                asset = selectedAsset
            }
    }

    data class InterestSummaryInfoItem(
        val title: String,
        val label: String
    )

    private data class CompositeInterestDetails(
        val balance: CryptoValue,
        val totalInterest: CryptoValue,
        val pendingInterest: CryptoValue,
        var nextInterestPayment: Date,
        val lockupDuration: Int,
        val interestRate: Double
    )

    override fun dismiss() {
        super.dismiss()
        disposables.clear()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        disposables.clear()
    }
}