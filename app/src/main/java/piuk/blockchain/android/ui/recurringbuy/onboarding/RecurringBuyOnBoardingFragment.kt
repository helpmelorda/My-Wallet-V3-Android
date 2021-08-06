package piuk.blockchain.android.ui.recurringbuy.onboarding

import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.LaunchOrigin
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentRecurringBuyOnBoardingBinding
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyAnalytics
import piuk.blockchain.android.urllinks.DOLLAR_COST_AVERAGING
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class RecurringBuyOnBoardingFragment : Fragment() {

    private var _binding: FragmentRecurringBuyOnBoardingBinding? = null
    private val binding: FragmentRecurringBuyOnBoardingBinding
        get() = _binding!!

    private val recurringBuyInfo: RecurringBuyInfo by unsafeLazy {
        arguments?.getParcelable(DATA) as? RecurringBuyInfo ?: throw IllegalStateException(
            "RecurringBuyInfo not provided"
        )
    }

    val analytics: Analytics by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecurringBuyOnBoardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.note.apply {
            recurringBuyInfo.noteLink?.let {
                visible()
                movementMethod = LinkMovementMethod.getInstance()
                text = setNote(it)
            } ?: gone()
        }
        binding.title.text = buildColorStrings()
    }

    private fun onClickLearnMore() {
        analytics.logEvent(
            RecurringBuyAnalytics.RecurringBuyLearnMoreClicked(LaunchOrigin.DCA_DETAILS_LINK)
        )
    }

    private fun setNote(stringId: Int): CharSequence {
        val linksMap = mapOf<String, Uri>(
            "learn_more" to Uri.parse(DOLLAR_COST_AVERAGING)
        )
        return StringUtils.getStringWithMappedAnnotations(
            requireContext(),
            stringId,
            linksMap
        ) { onClickLearnMore() }
    }

    private fun buildColorStrings(): Spannable {
        val title1 = recurringBuyInfo.title1
        val title2 = recurringBuyInfo.title2
        val sb = SpannableStringBuilder()
        sb.append(title1)
            .append(title2)
            .setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.grey_800)),
                0, title1.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        return sb
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DATA = "recurring_tx_buy_info"

        fun newInstance(recurringBuyOnBoardingInfo: RecurringBuyInfo): RecurringBuyOnBoardingFragment {
            return RecurringBuyOnBoardingFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(DATA, recurringBuyOnBoardingInfo)
                }
            }
        }
    }
}