package piuk.blockchain.android.ui.reset.password

import piuk.blockchain.android.ui.base.mvi.MviState

enum class ResetPasswordStatus {
    INIT,
    CREATE_WALLET,
    RECOVER_ACCOUNT,
    SET_PASSWORD,
    RESET_KYC,
    SHOW_ERROR,
    SHOW_SUCCESS
}

data class ResetPasswordState(
    val email: String = "",
    val password: String = "",
    val userId: String = "",
    val walletName: String = "",
    val recoveryPhrase: String = "",
    val recoveryToken: String = "",
    val status: ResetPasswordStatus = ResetPasswordStatus.INIT
) : MviState