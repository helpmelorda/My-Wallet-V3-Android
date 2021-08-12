package piuk.blockchain.android.ui.start

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AlertDialog
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.koin.scopedInject
import com.blockchain.koin.ssoAccountRecoveryFeatureFlag
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.connectivity.ConnectivityStatus
import piuk.blockchain.android.databinding.ActivityLandingBinding
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity
import piuk.blockchain.android.ui.createwallet.NewCreateWalletActivity
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.recover.AccountRecoveryActivity
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.android.urllinks.WALLET_STATUS_URL
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.copyHashOnLongClick
import piuk.blockchain.android.util.visible

class LandingActivity : MvpActivity<LandingView, LandingPresenter>(), LandingView {

    override val presenter: LandingPresenter by scopedInject()
    private val ssoARFF: FeatureFlag by inject(ssoAccountRecoveryFeatureFlag)
    private val internalFlags: InternalFeatureFlagApi by inject()
    private val compositeDisposable = CompositeDisposable()
    override val view: LandingView = this

    private val binding: ActivityLandingBinding by lazy {
        ActivityLandingBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding) {
            btnCreate.setOnClickListener { launchCreateWalletActivity() }

            if (!ConnectivityStatus.hasConnectivity(this@LandingActivity)) {
                showConnectivityWarning()
            } else {
                presenter.checkForRooted()
            }

            textVersion.text =
                "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.COMMIT_HASH}"

            textVersion.copyHashOnLongClick(this@LandingActivity)
        }
    }

    override fun onStart() {
        super.onStart()
        setupSSOControls()
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    private fun launchSSOAccountRecoveryFlow() =
        startActivity(Intent(this, AccountRecoveryActivity::class.java))

    private fun setupSSOControls() {
        with(binding) {
            btnLogin.setOnClickListener {
                launchSSOLoginActivity()
            }
            compositeDisposable += ssoARFF.enabled
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { isAccountRecoveryEnabled ->
                        btnRecover.apply {
                            if (isAccountRecoveryEnabled &&
                                internalFlags.isFeatureEnabled(GatedFeature.ACCOUNT_RECOVERY)
                            ) {
                                text = getString(R.string.restore_wallet_cta)
                                setOnClickListener { launchSSOAccountRecoveryFlow() }
                            } else {
                                text = getString(R.string.recover_funds)
                                setOnClickListener { showFundRecoveryWarning() }
                            }
                        }
                    },
                    onError = {
                        btnLogin.setOnClickListener { launchLoginActivity() }
                        btnRecover.apply {
                            text = getString(R.string.recover_funds)
                            setOnClickListener { showFundRecoveryWarning() }
                        }
                    }
                )
        }
    }

    private fun launchCreateWalletActivity() {
        if (internalFlags.isFeatureEnabled(GatedFeature.LOCALISATION_SIGN_UP)) {
            NewCreateWalletActivity.start(this)
        } else {
            CreateWalletActivity.start(this)
        }
    }

    private fun launchLoginActivity() =
        startActivity(Intent(this, LoginActivity::class.java))

    private fun launchSSOLoginActivity() =
        startActivity(Intent(this, piuk.blockchain.android.ui.login.LoginActivity::class.java))

    private fun startRecoverFundsActivity() = RecoverFundsActivity.start(this)

    private fun showConnectivityWarning() =
        showAlert(AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setMessage(getString(R.string.check_connectivity_exit))
            .setCancelable(false)
            .setNegativeButton(R.string.exit) { _, _ -> finishAffinity() }
            .setPositiveButton(R.string.retry) { _, _ ->
                val intent = Intent(this, LandingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .create()
        )

    private fun showFundRecoveryWarning() =
        showAlert(AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.recover_funds_warning_message_1)
            .setPositiveButton(R.string.dialog_continue) { _, _ -> startRecoverFundsActivity() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> clearAlert() }
            .create()
        )

    override fun showIsRootedWarning() =
        showAlert(AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setMessage(R.string.device_rooted)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_continue) { _, _ -> clearAlert() }
            .create()
        )

    override fun showApiOutageMessage() {
        binding.layoutWarning.root.visible()
        val learnMoreMap = mapOf<String, Uri>("learn_more" to Uri.parse(WALLET_STATUS_URL))
        binding.layoutWarning.warningMessage.apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = StringUtils.getStringWithMappedAnnotations(
                this@LandingActivity, R.string.wallet_issue_message, learnMoreMap
            )
        }
    }

    override fun showToast(message: String, toastType: String) = toast(message, toastType)

    companion object {
        @JvmStatic
        fun start(context: Context) {
            Intent(context, LandingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }
    }
}
