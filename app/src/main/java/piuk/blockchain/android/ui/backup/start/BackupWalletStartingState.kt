package piuk.blockchain.android.ui.backup.start

import piuk.blockchain.android.ui.base.mvi.MviState

enum class TriggerAlertStatus {
    INIT,
    ALERTING,
    COMPLETE
}

data class BackupWalletStartingState(val alertStatus: TriggerAlertStatus = TriggerAlertStatus.INIT) : MviState