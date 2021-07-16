package piuk.blockchain.android.ui.backup.start

import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class BackupWalletStartingIntents : MviIntent<BackupWalletStartingState> {

    object TriggerEmailAlert : BackupWalletStartingIntents() {
        override fun reduce(oldState: BackupWalletStartingState): BackupWalletStartingState =
            oldState.copy(
                alertStatus = TriggerAlertStatus.ALERTING
            )
    }

    object ShowComplete : BackupWalletStartingIntents() {
        override fun reduce(oldState: BackupWalletStartingState): BackupWalletStartingState =
            oldState.copy(
                alertStatus = TriggerAlertStatus.COMPLETE
            )
    }
}