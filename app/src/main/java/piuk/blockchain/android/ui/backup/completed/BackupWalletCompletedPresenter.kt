package piuk.blockchain.android.ui.backup.completed

import com.blockchain.preferences.WalletStatus
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import timber.log.Timber

interface BackupWalletCompletedView : View {
    fun showLastBackupDate(lastBackup: Long)
    fun hideLastBackupDate()
    fun onBackupDone()
    fun showErrorToast()
}

class BackupWalletCompletedPresenter(
    private val walletStatus: WalletStatus,
    private val authDataManager: AuthDataManager
) : BasePresenter<BackupWalletCompletedView>() {

    override fun onViewReady() {
        val lastBackup = walletStatus.lastBackupTime
        if (lastBackup != 0L) {
            view.showLastBackupDate(lastBackup)
        } else {
            view.hideLastBackupDate()
        }
    }

    fun updateMnemonicBackup() {
        compositeDisposable += authDataManager.updateMnemonicBackup()
            .subscribeBy(
                onComplete = { view.onBackupDone() },
                onError = { throwable ->
                    Timber.e(throwable)
                    view.showErrorToast()
                }
            )
    }
}
