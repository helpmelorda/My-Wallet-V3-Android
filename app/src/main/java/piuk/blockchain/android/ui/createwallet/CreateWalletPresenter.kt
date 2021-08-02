package piuk.blockchain.android.ui.createwallet

import android.app.LauncherActivity
import androidx.annotation.StringRes
import com.blockchain.api.nabu.data.GeolocationResponse
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.Logging
import com.blockchain.preferences.WalletStatus
import info.blockchain.wallet.util.PasswordUtil
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.domain.usecases.GetUserGeolocationUseCase
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.createwallet.NewCreateWalletActivity.Companion.CODE_US
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.FormatChecker
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrngFixer
import timber.log.Timber
import kotlin.math.roundToInt

interface CreateWalletView : View {
    fun showError(@StringRes message: Int)
    fun warnWeakPassword(email: String, password: String)
    fun startPinEntryActivity()
    fun showProgressDialog(message: Int)
    fun dismissProgressDialog()
    fun getDefaultAccountName(): String
    fun setGeolocationInCountrySpinner(geolocation: GeolocationResponse)
}

class CreateWalletPresenter(
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val appUtil: AppUtil,
    private val accessState: AccessState,
    private val prngFixer: PrngFixer,
    private val analytics: Analytics,
    private val walletPrefs: WalletStatus,
    private val environmentConfig: EnvironmentConfig,
    private val formatChecker: FormatChecker,
    private val getGeolocationUseCase: GetUserGeolocationUseCase
) : BasePresenter<CreateWalletView>() {

    override fun onViewReady() {
        // No-op
    }

    fun getUserGeolocation() {
        getGeolocationUseCase(Unit).subscribeBy(
            onSuccess = { geolocation ->
                view.setGeolocationInCountrySpinner(geolocation)
            },
            onError = {
                Timber.e(it.localizedMessage)
            }
        )
    }

    fun validateCredentials(email: String, password1: String, password2: String): Boolean =
        when {
            !formatChecker.isValidEmailAddress(email) -> {
                view.showError(R.string.invalid_email); false
            }
            password1.length < 4 -> {
                view.showError(R.string.invalid_password_too_short); false
            }
            password1.length > 255 -> {
                view.showError(R.string.invalid_password); false
            }
            password1 != password2 -> {
                view.showError(R.string.password_mismatch_error); false
            }
            !PasswordUtil.getStrength(password1).roundToInt().isStrongEnough() -> {
                view.warnWeakPassword(email, password1); false
            }
            else -> true
        }

    fun validateGeoLocation(countryCode: String? = null, stateCode: String? = null): Boolean =
        when {
            countryCode.isNullOrBlank() -> {
                view.showError(R.string.country_not_selected)
                false
            }
            countryCode == CODE_US && stateCode.isNullOrBlank() -> {
                view.showError(R.string.state_not_selected)
                false
            }
            else -> true
        }

    fun createOrRestoreWallet(email: String, password: String, recoveryPhrase: String) =
        when {
            recoveryPhrase.isNotEmpty() -> recoverWallet(email, password, recoveryPhrase)
            else -> createWallet(email, password)
        }

    private fun createWallet(email: String, password: String) {
        analytics.logEventOnce(AnalyticsEvents.WalletSignupCreated)
        prngFixer.applyPRNGFixes()

        compositeDisposable += payloadDataManager.createHdWallet(password, view.getDefaultAccountName(), email)
            .doOnSuccess {
                accessState.isNewlyCreated = true
                prefs.walletGuid = payloadDataManager.wallet!!.guid
                prefs.sharedKey = payloadDataManager.wallet!!.sharedKey
            }
            .doOnSubscribe { view.showProgressDialog(R.string.creating_wallet) }
            .doOnTerminate { view.dismissProgressDialog() }
            .subscribe(
                {
                    walletPrefs.setNewUser()
                    prefs.setValue(PersistentPrefs.KEY_EMAIL, email)
                    view.startPinEntryActivity()
                    Logging.logSignUp(true)
                    analytics.logEvent(AnalyticsEvents.WalletCreation)
                },
                {
                    Timber.e(it)
                    view.showError(R.string.hd_error)
                    appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
                    Logging.logSignUp(false)
                }
            )
    }

    private fun recoverWallet(email: String, password: String, recoveryPhrase: String) {
        compositeDisposable += payloadDataManager.restoreHdWallet(
            recoveryPhrase,
            view.getDefaultAccountName(),
            email,
            password
        ).doOnSuccess {
            accessState.isNewlyCreated = true
            accessState.isRestored = true
            prefs.walletGuid = payloadDataManager.wallet!!.guid
            prefs.sharedKey = payloadDataManager.wallet!!.sharedKey
        }.doOnSubscribe {
            view.showProgressDialog(R.string.restoring_wallet)
        }.doOnTerminate {
            view.dismissProgressDialog()
        }.subscribeBy(
            onSuccess = {
                prefs.setValue(PersistentPrefs.KEY_EMAIL, email)
                prefs.setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, true)
                view.startPinEntryActivity()
                analytics.logEvent(WalletCreationEvent.RecoverWalletEvent(true))
            },
            onError = {
                Timber.e(it)
                view.showError(R.string.restore_failed)
                analytics.logEvent(WalletCreationEvent.RecoverWalletEvent(false))
            }
        )
    }

    fun logEventEmailClicked() = analytics.logEventOnce(AnalyticsEvents.WalletSignupClickEmail)
    fun logEventPasswordOneClicked() = analytics.logEventOnce(AnalyticsEvents.WalletSignupClickPasswordFirst)
    fun logEventPasswordTwoClicked() = analytics.logEventOnce(AnalyticsEvents.WalletSignupClickPasswordSecond)

    private fun Int.isStrongEnough(): Boolean {
        val limit = if (environmentConfig.isRunningInDebugMode()) 1 else 50
        return this >= limit
    }
}