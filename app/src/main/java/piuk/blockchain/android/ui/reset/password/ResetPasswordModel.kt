package piuk.blockchain.android.ui.reset.password

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.models.responses.nabu.NabuApiException
import com.blockchain.nabu.models.responses.nabu.NabuErrorTypes
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

class ResetPasswordModel(
    initialState: ResetPasswordState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val interactor: ResetPasswordInteractor
) : MviModel<ResetPasswordState, ResetPasswordIntents>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(previousState: ResetPasswordState, intent: ResetPasswordIntents): Disposable? {
        return when (intent) {
            is ResetPasswordIntents.SetNewPassword ->
                setNewPassword(
                    password = intent.password,
                    intent.shouldResetKyc
                )
            is ResetPasswordIntents.CreateWalletForAccount ->
                createWalletForAccount(
                    email = intent.email,
                    password = intent.password,
                    recoveryToken = intent.recoveryToken,
                    walletName = intent.walletName,
                    intent.shouldResetKyc
                )
            is ResetPasswordIntents.RecoverAccount -> recoverAccount(intent.recoveryToken, intent.shouldResetKyc)
            ResetPasswordIntents.ResetUserKyc -> resetKyc()
            is ResetPasswordIntents.UpdateStatus -> null
        }
    }

    private fun setNewPassword(
        password: String,
        shouldResetKyc: Boolean
    ): Disposable {
        return interactor.setNewPassword(password = password)
            .subscribeBy(
                onComplete = {
                    if (shouldResetKyc) {
                        process(ResetPasswordIntents.ResetUserKyc)
                    } else {
                        process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS))
                    }
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR))
                }
            )
    }

    private fun createWalletForAccount(
        email: String,
        password: String,
        recoveryToken: String,
        walletName: String,
        shouldResetKyc: Boolean
    ): Disposable {
        return interactor.createWalletForAccount(email, password, walletName)
            .subscribeBy(
                onComplete = {
                    process(ResetPasswordIntents.RecoverAccount(recoveryToken, shouldResetKyc))
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR))
                }
            )
    }

    private fun recoverAccount(recoveryToken: String, shouldResetKyc: Boolean) =
        interactor.recoverAccount(recoveryToken = recoveryToken)
            .subscribeBy(
                onComplete = {
                    if (shouldResetKyc) {
                        process(ResetPasswordIntents.ResetUserKyc)
                    } else {
                        process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS))
                    }
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR))
                }
            )

    private fun resetKyc() = interactor.resetUserKyc()
        .subscribeBy(
            onComplete = { process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS)) },
            onError = { throwable ->
                if (throwable is NabuApiException && throwable.getErrorType() == NabuErrorTypes.Conflict) {
                    // Resetting KYC is already in progress
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS))
                } else {
                    Timber.e(throwable)
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR))
                }
            }
        )
}