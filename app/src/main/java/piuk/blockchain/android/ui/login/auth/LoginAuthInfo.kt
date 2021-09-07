package piuk.blockchain.android.ui.login.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class LoginAuthInfo {
    @Serializable
    data class SimpleAccountInfo(
        @SerialName("guid")
        val guid: String,
        @SerialName("email")
        val email: String,
        @SerialName("email_code")
        val authToken: String
    ) : LoginAuthInfo()

    @Serializable
    data class ExtendedAccountInfo(
        @SerialName("wallet")
        val accountWallet: AccountWalletInfo
    ) : LoginAuthInfo()
}

@Serializable
data class AccountWalletInfo(
    @SerialName("guid")
    val guid: String,
    @SerialName("email")
    val email: String,
    @SerialName("email_code")
    val authToken: String,
    @SerialName("nabu")
    val nabuAccountInfo: NabuAccountInfo = NabuAccountInfo()
)

@Serializable
data class NabuAccountInfo(
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("recovery_token")
    val recoveryToken: String = ""
)