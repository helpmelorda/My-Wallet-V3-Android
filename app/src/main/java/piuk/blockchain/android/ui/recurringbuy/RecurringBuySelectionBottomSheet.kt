package piuk.blockchain.android.ui.recurringbuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.utils.capitalizeFirstChar
import com.blockchain.utils.isLastDayOfTheMonth
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetRecurringBuyBinding
import piuk.blockchain.android.simplebuy.BuyFrequencySelected
import piuk.blockchain.android.simplebuy.SimpleBuyIntent
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.ui.base.HostedBottomSheet
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import java.time.ZonedDateTime

class RecurringBuySelectionBottomSheet : MviBottomSheet<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState,
    DialogSheetRecurringBuyBinding>() {

    interface Host : HostedBottomSheet.Host {
        fun onIntervalSelected(interval: RecurringBuyFrequency)
    }

    private val firstTimeAmountSpent: FiatValue? by lazy {
        arguments?.getSerializable(FIAT_AMOUNT_SPENT) as? FiatValue
    }

    private val cryptoCode: String? by lazy { arguments?.getString(CRYPTO_CODE) }

    private lateinit var selectedFrequency: RecurringBuyFrequency

    override val model: SimpleBuyModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetRecurringBuyBinding =
        DialogSheetRecurringBuyBinding.inflate(inflater, container, false)

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a RecurringBuySelectionBottomSheet.Host"
        )
    }

    override fun render(newState: SimpleBuyState) {
        val paymentMethodType = newState.selectedPaymentMethod?.paymentMethodType
        check(paymentMethodType != null)

        hideOrFillFrequencySelectorWithDates(
            newState.eligibleAndNextPaymentRecurringBuy,
            paymentMethodType
        )

        setPreselectedOrFirstFrequencyAvailable(
            currentFrequency = newState.recurringBuyFrequency,
            eligibleAndNextPaymentRecurringBuys = newState.eligibleAndNextPaymentRecurringBuy,
            paymentMethodType = paymentMethodType
        )
    }

    private fun isFirstTimeBuyer(): Boolean = firstTimeAmountSpent != null && cryptoCode != null

    private fun setPreselectedOrFirstFrequencyAvailable(
        currentFrequency: RecurringBuyFrequency,
        eligibleAndNextPaymentRecurringBuys: List<EligibleAndNextPaymentRecurringBuy>,
        paymentMethodType: PaymentMethodType
    ) {
        if (isFirstTimeBuyer()) {
            eligibleAndNextPaymentRecurringBuys.first { it.eligibleMethods.contains(paymentMethodType) }
                .let {
                    binding.recurringBuySelectionGroup.check(intervalToId(it.period))
                    selectedFrequency = it.period
                }
        } else {
            binding.recurringBuySelectionGroup.check(intervalToId(currentFrequency))
            selectedFrequency = currentFrequency
        }
    }

    private fun setViewForFirstTimeBuyer() {
        binding.apply {
            if (isFirstTimeBuyer()) {
                title.text = getString(
                    R.string.recurring_buy_first_time_title,
                    firstTimeAmountSpent!!.formatOrSymbolForZero(),
                    cryptoCode!!
                )
            } else {
                rbOneTime.visible()
            }
        }
    }

    override fun initControls(binding: DialogSheetRecurringBuyBinding) {
        setViewForFirstTimeBuyer()

        analytics.logEvent(RecurringBuyAnalytics.RecurringBuyViewed)

        with(binding) {
            recurringBuySelectionGroup.setOnCheckedChangeListener { _, checkedId ->
                selectedFrequency = idToInterval(checkedId)
                analytics.logEvent(
                    BuyFrequencySelected(
                        frequency = selectedFrequency.name
                    )
                )
            }
            recurringBuySelectCta.setOnClickListener {
                host.onIntervalSelected(selectedFrequency)
                dismiss()
            }
        }
    }

    private fun hideOrFillFrequencySelectorWithDates(
        eligibleAndNextPaymentRecurringBuys: List<EligibleAndNextPaymentRecurringBuy>,
        paymentMethodType: PaymentMethodType
    ) {

        eligibleAndNextPaymentRecurringBuys.forEach {
            binding.apply {
                when (it.period) {
                    RecurringBuyFrequency.DAILY -> {
                        if (it.eligibleMethods.contains(paymentMethodType)) {
                            rbDaily.visibleIf { it.eligibleMethods.contains(paymentMethodType) }
                        }
                    }
                    RecurringBuyFrequency.WEEKLY -> {
                        if (it.eligibleMethods.contains(paymentMethodType)) {
                            rbWeekly.apply {
                                visible()
                                text = getString(
                                    R.string.recurring_buy_frequency_subtitle,
                                    ZonedDateTime.parse(it.nextPaymentDate).dayOfWeek.toString().capitalizeFirstChar()
                                )
                            }
                        }
                    }
                    RecurringBuyFrequency.BI_WEEKLY -> {
                        if (it.eligibleMethods.contains(paymentMethodType)) {
                            rbBiWeekly.apply {
                                visible()
                                text = getString(
                                    R.string.recurring_buy_frequency_subtitle_biweekly,
                                    ZonedDateTime.parse(it.nextPaymentDate).dayOfWeek.toString().capitalizeFirstChar()
                                )
                            }
                        }
                    }
                    RecurringBuyFrequency.MONTHLY -> {
                        if (it.eligibleMethods.contains(paymentMethodType)) {
                            rbMonthly.apply {
                                visible()
                                text = if (ZonedDateTime.parse(it.nextPaymentDate).isLastDayOfTheMonth()) {
                                    getString(R.string.recurring_buy_frequency_subtitle_last_day_selector)
                                } else {
                                    getString(
                                        R.string.recurring_buy_frequency_subtitle_monthly,
                                        ZonedDateTime.parse(it.nextPaymentDate).dayOfMonth.toString()
                                    )
                                }
                            }
                        }
                    }
                    RecurringBuyFrequency.UNKNOWN, RecurringBuyFrequency.ONE_TIME -> {
                    }
                }
            }
        }
    }

    private fun intervalToId(interval: RecurringBuyFrequency) =
        when (interval) {
            RecurringBuyFrequency.DAILY -> R.id.rb_daily
            RecurringBuyFrequency.ONE_TIME -> R.id.rb_one_time
            RecurringBuyFrequency.WEEKLY -> R.id.rb_weekly
            RecurringBuyFrequency.BI_WEEKLY -> R.id.rb_bi_weekly
            RecurringBuyFrequency.MONTHLY -> R.id.rb_monthly
            RecurringBuyFrequency.UNKNOWN -> R.id.rb_one_time
        }

    private fun idToInterval(checkedId: Int) =
        when (checkedId) {
            R.id.rb_one_time -> RecurringBuyFrequency.ONE_TIME
            R.id.rb_daily -> RecurringBuyFrequency.DAILY
            R.id.rb_weekly -> RecurringBuyFrequency.WEEKLY
            R.id.rb_bi_weekly -> RecurringBuyFrequency.BI_WEEKLY
            R.id.rb_monthly -> RecurringBuyFrequency.MONTHLY
            else -> throw IllegalStateException("option selected RecurringBuyFrequency unknown")
        }

    companion object {
        const val PREVIOUS_SELECTED_STATE = "recurring_buy_check"
        const val FIAT_AMOUNT_SPENT = "fiat_amount_spent"
        const val CRYPTO_CODE = "crypto_asset_selected"
        fun newInstance(
            firstTimeAmountSpent: FiatValue? = null,
            cryptoValue: String? = null
        ): RecurringBuySelectionBottomSheet =
            RecurringBuySelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    if (firstTimeAmountSpent != null) putSerializable(FIAT_AMOUNT_SPENT, firstTimeAmountSpent)
                    if (cryptoValue != null) putSerializable(CRYPTO_CODE, cryptoValue)
                }
            }
    }
}