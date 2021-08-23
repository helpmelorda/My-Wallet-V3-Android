package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.logging.CrashLogger
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

internal data class PricesState(
    val availablePrices: List<AssetInfo> = emptyList(),
    val activeFlow: DialogFlow? = null,
    val selectedAsset: AssetInfo? = null
) : MviState

internal class PricesModel(
    initialState: PricesState,
    mainScheduler: Scheduler,
    private val interactor: PricesInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<PricesState, PricesIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(
        previousState: PricesState,
        intent: PricesIntent
    ): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")

        return null
    }
}
