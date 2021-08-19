package piuk.blockchain.android.ui.recurringbuy

import android.content.Context
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import piuk.blockchain.android.R
import piuk.blockchain.android.urllinks.URL_SUPPORT_BALANCE_LOCKED
import piuk.blockchain.android.util.StringUtils

fun PaymentMethodType.subtitleForLockedFunds(
    lockedFundDays: Long,
    context: Context
): SpannableStringBuilder {
    val intro = when (this) {
        PaymentMethodType.PAYMENT_CARD ->
            context.getString(
                R.string.security_locked_card_funds_explanation_1,
                lockedFundDays.toString()
            )
        PaymentMethodType.BANK_TRANSFER ->
            context.getString(
                R.string.security_locked_funds_bank_transfer_payment_screen_explanation,
                lockedFundDays.toString()
            )
        else -> return SpannableStringBuilder()
    }

    val map = mapOf("learn_more_link" to Uri.parse(URL_SUPPORT_BALANCE_LOCKED))

    val learnLink = StringUtils.getStringWithMappedAnnotations(
        context,
        R.string.common_linked_learn_more,
        map
    )

    val sb = SpannableStringBuilder()
    sb.append(intro)
        .append(learnLink)
        .setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue_600)),
            intro.length, intro.length + learnLink.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

    return sb
}