package piuk.blockchain.android.ui.reset

import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class ResetAccountIntents : MviIntent<ResetAccountState> {
    data class UpdateStatus(private val status: ResetAccountStatus) : ResetAccountIntents() {
        override fun reduce(oldState: ResetAccountState): ResetAccountState =
            oldState.copy(
                status = status
            )
    }
}