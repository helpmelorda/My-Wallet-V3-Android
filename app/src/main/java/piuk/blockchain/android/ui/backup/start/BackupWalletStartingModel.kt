package piuk.blockchain.android.ui.backup.start

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class BackupWalletStartingModel(
    initialState: BackupWalletStartingState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val interactor: BackupWalletStartingInteractor
) : MviModel<BackupWalletStartingState, BackupWalletStartingIntents>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(
        previousState: BackupWalletStartingState,
        intent: BackupWalletStartingIntents
    ): Disposable? {
        return when (intent) {
            BackupWalletStartingIntents.TriggerEmailAlert -> triggerEmailAlert()
            else -> null
        }
    }

    private fun triggerEmailAlert() =
        interactor.triggerSeedPhraseAlert()
            .subscribeBy(
                onComplete = { process(BackupWalletStartingIntents.UpdateStatus(BackupWalletStartingStatus.COMPLETE)) },
                onError = { process(BackupWalletStartingIntents.UpdateStatus(BackupWalletStartingStatus.COMPLETE)) }
            )
}