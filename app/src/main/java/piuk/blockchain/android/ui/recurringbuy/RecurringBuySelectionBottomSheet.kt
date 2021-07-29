package piuk.blockchain.android.ui.recurringbuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.utils.capitalizeFirstChar
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetRecurringBuyBinding
import piuk.blockchain.android.simplebuy.SimpleBuyIntent
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.ui.base.HostedBottomSheet
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visibleIf
import java.time.ZonedDateTime

class RecurringBuySelectionBottomSheet : MviBottomSheet<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState,
    DialogSheetRecurringBuyBinding>() {

    interface Host : HostedBottomSheet.Host {
        fun onIntervalSelected(interval: RecurringBuyFrequency)
    }

    private val interval: RecurringBuyFrequency by lazy {
        arguments?.getSerializable(PREVIOUS_SELECTED_STATE) as RecurringBuyFrequency
    }

    private val firstTimeAmountSpent: FiatValue? by lazy {
        arguments?.getSerializable(FIAT_AMOUNT_SPENT) as? FiatValue
    }

    private val cryptoCode: String? by lazy {
        arguments?.getString(CRYPTO_CODE)
    }

    private var selectedFrequency: RecurringBuyFrequency = RecurringBuyFrequency.ONE_TIME

    override val model: SimpleBuyModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetRecurringBuyBinding =
        DialogSheetRecurringBuyBinding.inflate(inflater, container, false)

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a RecurringBuySelectionBottomSheet.Host"
        )
    }

    override fun render(newState: SimpleBuyState) {
        binding.nextDatesLoading.visibleIf { newState.isLoading }
        fillFrequencySelectorWithDates(newState.nextPaymentDates)
    }

    private fun setViewForFirstTimeBuyer() {
        selectedFrequency = RecurringBuyFrequency.DAILY
        if (firstTimeAmountSpent != null && cryptoCode != null) {
            binding.apply {
                rbOneTime.gone()
                title.text = getString(
                    R.string.recurring_buy_first_time_title,
                    firstTimeAmountSpent!!.formatOrSymbolForZero(),
                    cryptoCode!!
                )
            }
        }
    }

    override fun initControls(binding: DialogSheetRecurringBuyBinding) {
        model.process(SimpleBuyIntent.LoadNextPaymentDates)

        setViewForFirstTimeBuyer()

        analytics.logEvent(RecurringBuyAnalytics.RecurringBuyViewed)

        with(binding) {
            recurringBuySelectionGroup.check(intervalToId(interval))
            recurringBuySelectionGroup.setOnCheckedChangeListener { _, checkedId ->
                selectedFrequency = idToInterval(checkedId)
            }
            recurringBuySelectCta.setOnClickListener {
                host.onIntervalSelected(selectedFrequency)
                dismiss()
            }
        }
    }

    private fun fillFrequencySelectorWithDates(nextPaymentMap: Map<RecurringBuyFrequency, ZonedDateTime>) {
        if (nextPaymentMap.isNotEmpty()) {
            binding.apply {
                rbWeekly.text = getString(
                    R.string.recurring_buy_frequency_subtitle,
                    nextPaymentMap[RecurringBuyFrequency.WEEKLY]?.dayOfWeek.toString().capitalizeFirstChar()
                )
                rbMonthly.text = getString(
                    R.string.recurring_buy_frequency_subtitle_monthly,
                    nextPaymentMap[RecurringBuyFrequency.MONTHLY]?.dayOfMonth.toString()
                )
                rbBiWeekly.text = getString(
                    R.string.recurring_buy_frequency_subtitle_biweekly,
                    nextPaymentMap[RecurringBuyFrequency.BI_WEEKLY]?.dayOfWeek.toString().capitalizeFirstChar()
                )
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
            interval: RecurringBuyFrequency,
            firstTimeAmountSpent: FiatValue? = null,
            cryptoValue: String? = null
        ): RecurringBuySelectionBottomSheet =
            RecurringBuySelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(PREVIOUS_SELECTED_STATE, interval)
                    if (firstTimeAmountSpent != null) putSerializable(FIAT_AMOUNT_SPENT, firstTimeAmountSpent)
                    if (cryptoValue != null) putSerializable(CRYPTO_CODE, cryptoValue)
                }
            }
    }
}