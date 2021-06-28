package piuk.blockchain.android.ui.backup.wordlist

import android.os.Bundle
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment.Companion.ARGUMENT_SECOND_PASSWORD
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.util.BackupWalletUtil

interface BackupWalletWordListView : View {
    fun getPageBundle(): Bundle?
    fun finish()
}

class BackupWalletWordListPresenter(
    private val backupWalletUtil: BackupWalletUtil
) : BasePresenter<BackupWalletWordListView>() {

    internal var secondPassword: String? = null
    private var mnemonic: List<String>? = null

    override fun onViewReady() {
        val bundle = view.getPageBundle()
        secondPassword = bundle?.getString(ARGUMENT_SECOND_PASSWORD)

        mnemonic = backupWalletUtil.getMnemonic(secondPassword)
        if (mnemonic == null) {
            view.finish()
        }
    }

    internal fun getWordForIndex(index: Int) = mnemonic!![index]

    internal fun getMnemonicSize() = mnemonic?.size ?: -1
}