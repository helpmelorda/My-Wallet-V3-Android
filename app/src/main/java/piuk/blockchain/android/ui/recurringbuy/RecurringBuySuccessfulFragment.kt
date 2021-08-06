package piuk.blockchain.android.ui.recurringbuy

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.getColor
import com.blockchain.koin.scopedInject
import com.blockchain.utils.secondsToDays
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentRecurringBuySuccessfulBinding
import piuk.blockchain.android.simplebuy.SimpleBuyIntent
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.setAssetIconColours
import java.util.Locale

class RecurringBuySuccessfulFragment :
    MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState, FragmentRecurringBuySuccessfulBinding>() {

    override val model: SimpleBuyModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRecurringBuySuccessfulBinding =
        FragmentRecurringBuySuccessfulBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.setupToolbar(R.string.recurring_buy_first_time_toolbar, false)

        binding.apply {
            icon.setAssetIconColours(
                tintColor = getColor(requireContext(), R.color.blue_600),
                filterColor = getColor(requireContext(), R.color.white)
            )
            okBtn.setOnClickListener { requireActivity().finish() }
        }
    }

    override fun render(state: SimpleBuyState) {
        val lockedFundDays = state.withdrawalLockPeriod.secondsToDays()
        if (lockedFundDays > 0L) {
            require(state.selectedPaymentMethod != null)
            val locksNote = state.selectedPaymentMethod.paymentMethodType
                .subtitleForLockedFunds(
                    lockedFundDays,
                    requireContext()
                )
            with(binding.noteSuccessRb) {
                setText(locksNote, TextView.BufferType.SPANNABLE)
                movementMethod = LinkMovementMethod.getInstance()
            }
        } else {
            binding.noteSuccessRb.gone()
        }
        binding.subtitleSuccessRb.text = getString(
            R.string.recurring_buy_first_time_success_subtitle,
            state.order.amount?.formatOrSymbolForZero(),
            state.selectedCryptoAsset?.name,
            state.recurringBuyFrequency
                .toHumanReadableRecurringBuy(requireContext())
                .toLowerCase(Locale.getDefault())
        )
    }
}