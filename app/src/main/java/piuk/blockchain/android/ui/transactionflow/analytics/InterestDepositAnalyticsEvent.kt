package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.notifications.analytics.AnalyticsEvent
import info.blockchain.balance.AssetInfo

sealed class InterestDepositAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {
    object EnterAmountSeen : InterestDepositAnalyticsEvent("earn_amount_screen_seen")
    object ConfirmationsSeen : InterestDepositAnalyticsEvent("earn_checkout_shown")
    object CancelTransaction : InterestDepositAnalyticsEvent("earn_checkout_cancel")

    data class ConfirmationsCtaClick(
        val asset: AssetInfo
    ) : InterestDepositAnalyticsEvent("earn_deposit_confirm_click", params = mapOf("asset" to asset.ticker))

    data class EnterAmountCtaClick(
        val asset: AssetInfo
    ) : InterestDepositAnalyticsEvent("earn_amount_screen_confirm", params = mapOf("asset" to asset.ticker))

    data class TransactionSuccess(
        val asset: AssetInfo
    ) : InterestDepositAnalyticsEvent("earn_checkout_success", params = mapOf("asset" to asset.ticker))

    data class TransactionFailed(
        val asset: AssetInfo
    ) : InterestDepositAnalyticsEvent("earn_checkout_error", params = mapOf("asset" to asset.ticker))
}
