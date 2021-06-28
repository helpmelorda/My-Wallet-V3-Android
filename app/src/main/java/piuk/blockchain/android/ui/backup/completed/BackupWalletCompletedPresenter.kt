package piuk.blockchain.android.ui.backup.completed

import com.blockchain.preferences.WalletStatus
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.View

interface BackupWalletCompletedView : View {
    fun showLastBackupDate(lastBackup: Long)
    fun hideLastBackupDate()
}

class BackupWalletCompletedPresenter(
    private val walletStatus: WalletStatus
) : BasePresenter<BackupWalletCompletedView>() {

    override fun onViewReady() {
        val lastBackup = walletStatus.lastBackupTime
        if (lastBackup != 0L) {
            view.showLastBackupDate(lastBackup)
        } else {
            view.hideLastBackupDate()
        }
    }
}
