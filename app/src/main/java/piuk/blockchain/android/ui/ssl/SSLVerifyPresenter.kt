package piuk.blockchain.android.ui.ssl

import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.androidcore.utils.SSLVerifyUtil

class SSLVerifyPresenter(
    private val sslVerifyUtil: SSLVerifyUtil
) : BasePresenter<SSLVerifyView>() {

    override fun onViewReady() {
        view.showWarningPrompt()
    }

    fun validateSSL() {
        sslVerifyUtil.validateSSL()
    }
}