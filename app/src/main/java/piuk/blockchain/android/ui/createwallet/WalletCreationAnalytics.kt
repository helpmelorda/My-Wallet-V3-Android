package piuk.blockchain.android.ui.createwallet

import com.blockchain.extensions.withoutNullValues
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames

sealed class WalletCreationAnalytics(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    class WalletSignUp(
        countryIso: String,
        stateIso: String?
    ) : WalletCreationAnalytics(
        event = AnalyticsNames.WALLET_SIGN_UP.eventName,
        params = mapOf(
            COUNTRY to countryIso,
            STATE to stateIso
        ).withoutNullValues()
    )

    class CountrySelectedOnSignUp(
        countryIso: String
    ) : WalletCreationAnalytics(
        event = AnalyticsNames.WALLET_SIGN_UP_COUNTRY_SELECTED.eventName,
        params = mapOf(COUNTRY to countryIso)
    )

    class StateSelectedOnSignUp(
        stateIso: String
    ) : WalletCreationAnalytics(
        event = AnalyticsNames.WALLET_SIGN_UP_STATE_SELECTED.eventName,
        params = mapOf(STATE to stateIso)
    )

    class RecoverWalletAnalytics(success: Boolean) : WalletCreationAnalytics(
        event = AnalyticsNames.WALLET_RECOVER.eventName,
        mapOf("Success" to success.toString())
    )

    companion object {
        private const val COUNTRY = "country"
        private const val STATE = "country_state"
    }
}
