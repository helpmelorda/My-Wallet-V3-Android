package piuk.blockchain.android.ui.login.auth

import piuk.blockchain.android.ui.base.mvi.MviState

enum class TwoFAMethod(private val value: Int) {
    OFF(0),
    YUBI_KEY(1),
    GOOGLE_AUTHENTICATOR(4),
    SMS(5),
    SECOND_PASSWORD(6);

    companion object {
        fun fromInt(v: Int): TwoFAMethod = values().firstOrNull { it.value == v } ?: OFF
    }
}

enum class AuthStatus {
    None,
    GetSessionId,
    AuthorizeApproval,
    GetPayload,
    VerifyPassword,
    Submit2FA,
    UpdateMobileSetup,
    Complete,
    PairingFailed,
    InvalidPassword,
    Invalid2FACode,
    AuthRequired,
    AuthFailed,
    InitialError,
    ShowManualPairing,
    AccountLocked
}

data class LoginAuthState(
    val guid: String = "",
    val authToken: String = "",
    val password: String = "",
    val sessionId: String = "",
    val authStatus: AuthStatus = AuthStatus.None,
    val authMethod: TwoFAMethod = TwoFAMethod.OFF,
    val payloadJson: String = "",
    val code: String = "",
    val isMobileSetup: Boolean = false,
    val deviceType: Int = 0,
    val twoFaState: TwoFaCodeState? = null
) : MviState {
    companion object {
        const val TWO_FA_COUNTDOWN = 60000L
        const val TWO_FA_STEP = 1000L
    }
}

sealed class TwoFaCodeState {
    class TwoFaRemainingTries(val remainingRetries: Int) : TwoFaCodeState()
    object TwoFaTimeLock : TwoFaCodeState()
}