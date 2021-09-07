package piuk.blockchain.android.ui.login.auth

import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class LoginAuthIntents : MviIntent<LoginAuthState> {

    data class InitLoginAuthInfo(val json: String) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.InitAuthInfo
            )
    }

    data class GetSessionId(val loginAuthInfo: LoginAuthInfo) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            when (loginAuthInfo) {
                is LoginAuthInfo.SimpleAccountInfo -> oldState.copy(
                    guid = loginAuthInfo.guid,
                    email = loginAuthInfo.email,
                    authToken = loginAuthInfo.authToken,
                    authStatus = AuthStatus.GetSessionId
                )
                is LoginAuthInfo.ExtendedAccountInfo -> oldState.copy(
                    guid = loginAuthInfo.accountWallet.guid,
                    userId = loginAuthInfo.accountWallet.nabuAccountInfo.userId,
                    email = loginAuthInfo.accountWallet.email,
                    authToken = loginAuthInfo.accountWallet.authToken,
                    recoveryToken = loginAuthInfo.accountWallet.nabuAccountInfo.recoveryToken,
                    authStatus = AuthStatus.GetSessionId
                )
            }
    }

    data class AuthorizeApproval(val sessionId: String) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                sessionId = sessionId,
                authStatus = AuthStatus.AuthorizeApproval
            )
    }

    object GetPayload : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.GetPayload
            )
    }

    data class SetPayload(val payloadJson: JsonObject) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authMethod = getAuthMethod(oldState),
                payloadJson = payloadJson.toString()
            )

        private fun getAuthMethod(oldState: LoginAuthState): TwoFAMethod {
            return if (payloadJson.isAuth() && (payloadJson.isGoogleAuth() || payloadJson.isSMSAuth())) {
                TwoFAMethod.fromInt(payloadJson.getValue(AUTH_TYPE).jsonPrimitive.toString().toInt())
            } else {
                oldState.authMethod
            }
        }
    }

    data class Update2FARetryCount(val count: Int) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                twoFaState = when (count) {
                    0 -> TwoFaCodeState.TwoFaTimeLock
                    else -> TwoFaCodeState.TwoFaRemainingTries(count)
                }
            )
    }

    object RequestNew2FaCode : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState = oldState
    }

    object New2FaCodeTimeLock : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState = oldState.copy(
            twoFaState = TwoFaCodeState.TwoFaTimeLock
        )

        override fun isValidFor(oldState: LoginAuthState): Boolean = true
    }

    data class SubmitTwoFactorCode(val password: String, val code: String) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                password = password,
                code = code,
                authStatus = AuthStatus.Submit2FA
            )
    }

    data class VerifyPassword(val password: String, val payloadJson: String = "") : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                password = password,
                payloadJson = if (payloadJson.isNotEmpty()) payloadJson else oldState.payloadJson,
                authStatus = AuthStatus.VerifyPassword
            )
    }

    data class UpdateMobileSetup(val isMobileSetup: Boolean, val deviceType: Int) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.UpdateMobileSetup,
                isMobileSetup = isMobileSetup,
                deviceType = deviceType
            )
    }

    object ShowAuthComplete : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.Complete
            )
    }

    object ShowInitialError : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.InitialError
            )
    }

    object ShowAuthRequired : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.AuthRequired
            )
    }

    object ShowAccountLockedError : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.AccountLocked
            )
    }

    object Show2FAFailed : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.Invalid2FACode
            )
    }

    object Reset2FARetries : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState
    }

    data class ShowError(val throwable: Throwable? = null) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = when (throwable) {
                    is HDWalletException -> AuthStatus.PairingFailed
                    is DecryptionException -> AuthStatus.InvalidPassword
                    else -> AuthStatus.AuthFailed
                }
            )
    }

    class ShowManualPairing(private val guid: String?) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.ShowManualPairing,
                guid = guid.orEmpty()
            )
    }

    companion object {
        const val AUTH_TYPE = "auth_type"
        const val PAYLOAD = "payload"
    }
}

private fun JsonObject.isAuth(): Boolean =
    containsKey(LoginAuthIntents.AUTH_TYPE) && !containsKey(LoginAuthIntents.PAYLOAD)

private fun JsonObject.isGoogleAuth(): Boolean =
    getValue(LoginAuthIntents.AUTH_TYPE).jsonPrimitive.toString().toInt() == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR

private fun JsonObject.isSMSAuth(): Boolean =
    getValue(LoginAuthIntents.AUTH_TYPE).jsonPrimitive.toString().toInt() == Settings.AUTH_TYPE_SMS