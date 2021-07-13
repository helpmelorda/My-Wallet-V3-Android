package piuk.blockchain.android.ui.reset

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class ResetAccountModel(
    initialState: ResetAccountState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<ResetAccountState, ResetAccountIntents>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(
        previousState: ResetAccountState,
        intent: ResetAccountIntents
    ): Disposable? = null
}