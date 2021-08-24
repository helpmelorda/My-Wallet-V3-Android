package piuk.blockchain.android.ui.createwallet

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames

sealed class WalletCreationEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object WalletSignUp : WalletCreationEvent(
        AnalyticsNames.WALLET_SIGN_UP.eventName
    )

    class RecoverWalletEvent(success: Boolean) : WalletCreationEvent(
        "Recover Wallet",
        mapOf("Success" to success.toString())
    )
}
