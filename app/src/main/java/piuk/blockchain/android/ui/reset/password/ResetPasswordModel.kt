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
                    userId = intent.userId,
                    recoveryToken = intent.recoveryToken,
                    walletName = intent.walletName,
                    intent.shouldResetKyc
                )
            is ResetPasswordIntents.RecoverAccount ->
                recoverAccount(
                    userId = intent.userId,
                    recoveryToken = intent.recoveryToken,
                    shouldResetKyc = intent.shouldResetKyc
                )
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
                onComplete = { resetKycOrContinue(shouldResetKyc) },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR))
                }
            )
    }

    private fun createWalletForAccount(
        email: String,
        password: String,
        userId: String,
        recoveryToken: String,
        walletName: String,
        shouldResetKyc: Boolean
    ): Disposable {
        return interactor.createWalletForAccount(email, password, walletName)
            .subscribeBy(
                onComplete = {
                    process(
                        ResetPasswordIntents.RecoverAccount(
                            userId = userId,
                            recoveryToken = recoveryToken,
                            shouldResetKyc = shouldResetKyc
                        )
                    )
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR))
                }
            )
    }

    private fun recoverAccount(userId: String, recoveryToken: String, shouldResetKyc: Boolean) =
        interactor.recoverAccount(userId = userId, recoveryToken = recoveryToken)
            .subscribeBy(
                onComplete = { resetKycOrContinue(shouldResetKyc) },
                onError = { throwable ->
                    process(
                        when {
                            shouldResetKyc && isErrorResponseConflict(throwable) ->
                                ResetPasswordIntents.ResetUserKyc
                            isErrorResponseConflict(throwable) ->
                                ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS)
                            else -> {
                                Timber.e(throwable)
                                ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR)
                            }
                        }
                    )
                }
            )

    private fun resetKyc() = interactor.resetUserKyc()
        .subscribeBy(
            onComplete = { process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS)) },
            onError = { throwable ->
                if (isErrorResponseConflict(throwable)) {
                    // Resetting KYC is already in progress
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS))
                } else {
                    Timber.e(throwable)
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR))
                }
            }
        )

    private fun resetKycOrContinue(shouldResetKyc: Boolean) {
        process(
            if (shouldResetKyc) {
                ResetPasswordIntents.ResetUserKyc
            } else {
                ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS)
            }
        )
    }

    // Recovery/Reset is already in progress
    private fun isErrorResponseConflict(throwable: Throwable) =
        throwable is NabuApiException && throwable.getErrorType() == NabuErrorTypes.Conflict
}