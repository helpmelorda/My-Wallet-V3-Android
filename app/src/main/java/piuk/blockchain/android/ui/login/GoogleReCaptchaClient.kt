package piuk.blockchain.android.ui.login

import android.app.Activity
import com.google.android.gms.recaptcha.Recaptcha
import com.google.android.gms.recaptcha.RecaptchaAction
import com.google.android.gms.recaptcha.RecaptchaActionType
import com.google.android.gms.recaptcha.RecaptchaHandle
import com.google.android.gms.recaptcha.RecaptchaResultData
import info.blockchain.wallet.api.Environment
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

class GoogleReCaptchaClient(private val activity: Activity, private val environmentConfig: EnvironmentConfig) {
    private lateinit var recaptchaHandle: RecaptchaHandle

    fun initReCaptcha() {
        Recaptcha.getClient(activity)
            .init(BuildConfig.RECAPTCHA_SITE_KEY)
            .addOnSuccessListener { handle ->
                recaptchaHandle = handle
            }
            .addOnFailureListener { exception ->
                Timber.e(exception)
            }
    }

    fun close() {
        if (this::recaptchaHandle.isInitialized) {
            Recaptcha.getClient(activity).close(recaptchaHandle)
        }
    }

    fun verifyForLogin(onSuccess: (RecaptchaResultData) -> Unit, onError: (Exception) -> Unit) {
        if (this::recaptchaHandle.isInitialized) {
            Recaptcha.getClient(activity)
                .execute(recaptchaHandle, RecaptchaAction(RecaptchaActionType(RecaptchaActionType.LOGIN)))
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onError)
        } else {
            // workaround for getting past re-captcha on staging
            if (environmentConfig.isRunningInDebugMode() && environmentConfig.environment == Environment.STAGING) {
                onSuccess(RecaptchaResultData(DUMMY_RESULT))
            }
        }
    }

    companion object {
        private const val DUMMY_RESULT = "1234"
    }
}