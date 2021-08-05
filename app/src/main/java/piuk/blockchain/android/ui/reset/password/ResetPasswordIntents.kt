package piuk.blockchain.android.ui.reset.password

import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class ResetPasswordIntents : MviIntent<ResetPasswordState> {

    data class SetNewPassword(
        val password: String,
        val shouldResetKyc: Boolean
    ) : ResetPasswordIntents() {
        override fun reduce(oldState: ResetPasswordState): ResetPasswordState =
            oldState.copy(
                password = password,
                status = ResetPasswordStatus.SET_PASSWORD
            )
    }

    data class UpdateStatus(private val status: ResetPasswordStatus) : ResetPasswordIntents() {
        override fun reduce(oldState: ResetPasswordState): ResetPasswordState = oldState.copy(status = status)
    }

    data class RecoverAccount(
        val recoveryToken: String,
        val shouldResetKyc: Boolean
    ) : ResetPasswordIntents() {
        override fun reduce(oldState: ResetPasswordState): ResetPasswordState =
            oldState.copy(
                recoveryToken = recoveryToken,
                status = ResetPasswordStatus.RECOVER_ACCOUNT
            )
    }

    data class CreateWalletForAccount(
        val email: String,
        val password: String,
        val recoveryToken: String,
        val walletName: String,
        val shouldResetKyc: Boolean
    ) : ResetPasswordIntents() {
        override fun reduce(oldState: ResetPasswordState): ResetPasswordState =
            oldState.copy(
                email = email,
                password = password,
                recoveryToken = recoveryToken,
                walletName = walletName,
                status = ResetPasswordStatus.CREATE_WALLET
            )
    }

    object ResetUserKyc : ResetPasswordIntents() {
        override fun reduce(oldState: ResetPasswordState): ResetPasswordState =
            oldState.copy(status = ResetPasswordStatus.RESET_KYC)
    }
}