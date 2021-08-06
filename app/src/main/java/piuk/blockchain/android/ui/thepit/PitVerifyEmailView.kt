package piuk.blockchain.android.ui.thepit

import piuk.blockchain.android.ui.base.View

interface PitVerifyEmailView : View {
    fun mailResendFailed()
    fun mailResentSuccessfully()
    fun emailVerified()
}