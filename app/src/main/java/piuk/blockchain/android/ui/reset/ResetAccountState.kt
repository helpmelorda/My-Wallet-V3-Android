package piuk.blockchain.android.ui.reset

import piuk.blockchain.android.ui.base.mvi.MviState

enum class ResetAccountStatus {
    SHOW_INFO,
    RETRY,
    SHOW_WARNING,
    RESET
}

data class ResetAccountState(val status: ResetAccountStatus = ResetAccountStatus.SHOW_INFO) : MviState