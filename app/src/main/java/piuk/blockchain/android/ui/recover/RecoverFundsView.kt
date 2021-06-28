package piuk.blockchain.android.ui.recover

import androidx.annotation.StringRes
import piuk.blockchain.android.ui.base.View

interface RecoverFundsView : View {

    fun showError(@StringRes message: Int)

    fun showProgressDialog(@StringRes messageId: Int)

    fun dismissProgressDialog()

    fun startPinEntryActivity()

    fun gotoCredentialsActivity(recoveryPhrase: String)
}
