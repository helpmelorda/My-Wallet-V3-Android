package piuk.blockchain.android.ui.recurringbuy.onboarding

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecurringBuyInfo(
    val title1: String,
    val title2: String,
    val noteLink: Int? = null
) : Parcelable