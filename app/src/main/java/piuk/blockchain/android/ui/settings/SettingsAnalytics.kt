package piuk.blockchain.android.ui.settings

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.notifications.analytics.LaunchOrigin
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalyticsAccountType

sealed class SettingsAnalytics(override val event: String, override val params: Map<String, String> = mapOf()) :
    AnalyticsEvent {

    object EmailClicked : SettingsAnalytics("settings_email_clicked")
    object PhoneClicked : SettingsAnalytics("settings_phone_clicked")
    object SwapLimitChecked : SettingsAnalytics("settings_swap_limit_clicked")
    object CloudBackupSwitch : SettingsAnalytics("settings_cloud_backup_switch")
    object WalletIdCopyClicked : SettingsAnalytics("settings_wallet_id_copy_click")
    object WalletIdCopyCopied : SettingsAnalytics("settings_wallet_id_copied")
    object EmailNotificationClicked : SettingsAnalytics("settings_email_notif_switch")
    object ChangePassClicked : SettingsAnalytics("settings_password_click")
    object TwoFactorAuthClicked : SettingsAnalytics("settings_two_fa_click")
    object ChangePinClicked_Old : SettingsAnalytics("settings_change_pin_click")
    object BiometryAuthSwitch : SettingsAnalytics("settings_biometry_auth_switch")
    object PinChanged_Old : SettingsAnalytics("settings_pin_selected")
    object PasswordChanged_Old : SettingsAnalytics("settings_password_selected")
    object CurrencyChanged : SettingsAnalytics("settings_currency_selected")
    object MobileChangeClicked : SettingsAnalytics(AnalyticsNames.CHANGE_MOBILE_NUMBER_CLICKED.eventName)
    object NotificationPrefsUpdated : SettingsAnalytics(AnalyticsNames.NOTIFICATION_PREFS_UPDATED.eventName)

    class PasswordChanged(txFlowAccountType: TxFlowAnalyticsAccountType) : SettingsAnalytics(
        AnalyticsNames.ACCOUNT_PASSWORD_CHANGED.eventName,
        mapOf(
            "account_type" to txFlowAccountType.name
        )
    )

    object ChangePinClicked : SettingsAnalytics(
        AnalyticsNames.CHANGE_PIN_CODE_CLICKED.eventName
    )

    object EmailChangeClicked : SettingsAnalytics(
        AnalyticsNames.CHANGE_EMAIL_CLICKED.eventName
    )

    class BiometricsOptionUpdated(isEnabled: Boolean) : SettingsAnalytics(
        AnalyticsNames.BIOMETRICS_OPTION_UPDATED.eventName,
        mapOf(
            "is_enabled" to isEnabled.toString()
        )
    )

    object PinCodeChanged : SettingsAnalytics(
        AnalyticsNames.PIN_CODE_CHANGED.eventName
    )

    object RecoveryPhraseShown : SettingsAnalytics(
        AnalyticsNames.RECOVERY_PHRASE_SHOWN.eventName
    )

    class TwoStepVerificationClicked(option: String) : SettingsAnalytics(
        AnalyticsNames.TWO_STEP_VERIFICATION_CODE_CLICKED.eventName,
        mapOf(
            TWO_STEP_OPTION to option
        )
    )

    class TwoStepVerificationCodeSubmitted(option: String) : SettingsAnalytics(
        AnalyticsNames.TWO_STEP_VERIFICATION_CODE_SUBMITTED.eventName,
        mapOf(
            TWO_STEP_OPTION to option
        )
    )

    class LinkCardClicked(override val origin: LaunchOrigin) : SettingsAnalytics(
        AnalyticsNames.LINK_CARD_CLICKED.eventName
    )

    class RemoveCardClicked(override val origin: LaunchOrigin) :
        SettingsAnalytics(AnalyticsNames.REMOVE_CARD_CLICKED.eventName)

    class SettingsHyperlinkClicked(private val destination: AnalyticsHyperlinkDestination) :
        SettingsAnalytics(AnalyticsNames.SETTINGS_HYPERLINK_DESTINATION.eventName)

    companion object {
        const val TWO_SET_MOBILE_NUMBER_OPTION = "Mobile Number"
        private const val TWO_STEP_OPTION = "two_step_option"
    }

    enum class AnalyticsHyperlinkDestination {
        ABOUT, PRIVACY_POLICY, TERMS_OF_SERVICE
    }
}