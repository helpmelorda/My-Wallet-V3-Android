package piuk.blockchain.android.ui.login

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.json.JSONObject
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber
import javax.net.ssl.SSLPeerUnverifiedException

class LoginModel(
    initialState: LoginState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val interactor: LoginInteractor
) : MviModel<LoginState, LoginIntents>(initialState, mainScheduler, environmentConfig, crashLogger) {

    override fun performAction(previousState: LoginState, intent: LoginIntents): Disposable? {
        return when (intent) {
            is LoginIntents.LoginWithQr -> loginWithQrCode(intent.qrString)
            is LoginIntents.ObtainSessionIdForEmail ->
                obtainSessionId(
                    intent.selectedEmail,
                    intent.captcha
                )
            is LoginIntents.SendEmail ->
                sendVerificationEmail(
                    intent.sessionId,
                    intent.selectedEmail,
                    intent.captcha
                )
            else -> null
        }
    }

    private fun loginWithQrCode(qrString: String): Disposable {

        return interactor.loginWithQrCode(qrString)
            .subscribeBy(
                onComplete = {
                    process(LoginIntents.StartPinEntry)
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(
                        LoginIntents.ShowScanError(
                            shouldRestartApp = throwable is SSLPeerUnverifiedException
                        )
                    )
                }
            )
    }

    private fun obtainSessionId(email: String, captcha: String): Disposable {

        return interactor.obtainSessionId(email)
            .subscribeBy(
                onSuccess = { responseBody ->
                    val response = JSONObject(responseBody.string())
                    if (response.has(SESSION_TOKEN)) {
                        val sessionId = response.getString(SESSION_TOKEN)
                        process(LoginIntents.SendEmail(sessionId, email, captcha))
                    } else {
                        process(LoginIntents.GetSessionIdFailed)
                    }
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(LoginIntents.GetSessionIdFailed) }
            )
    }

    private fun sendVerificationEmail(
        sessionId: String,
        email: String,
        captcha: String
    ): Disposable {
        return interactor.sendEmailForVerification(sessionId, email, captcha)
            .subscribeBy(
                onComplete = { process(LoginIntents.ShowEmailSent) },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(LoginIntents.ShowEmailFailed) }
            )
    }

    companion object {
        private const val SESSION_TOKEN = "token"
    }
}