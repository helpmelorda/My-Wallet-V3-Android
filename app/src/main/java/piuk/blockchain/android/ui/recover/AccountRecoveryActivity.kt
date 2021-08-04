package piuk.blockchain.android.ui.recover

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import androidx.annotation.StringRes
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityAccountRecoveryBinding
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.addAnimationTransaction
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.reset.ResetAccountFragment
import piuk.blockchain.android.ui.reset.password.ResetPasswordFragment
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.visibleIf

class AccountRecoveryActivity :
    MviActivity<AccountRecoveryModel, AccountRecoveryIntents, AccountRecoveryState, ActivityAccountRecoveryBinding>() {

    override val model: AccountRecoveryModel by scopedInject()

    override val alwaysDisableScreenshots: Boolean
        get() = true

    private val email: String by lazy {
        intent.getStringExtra(ResetPasswordFragment.EMAIL) ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initControls()
    }

    override fun initBinding(): ActivityAccountRecoveryBinding = ActivityAccountRecoveryBinding.inflate(layoutInflater)

    override fun render(newState: AccountRecoveryState) {
        when (newState.status) {
            AccountRecoveryStatus.INVALID_PHRASE ->
                showSeedPhraseInputError(R.string.invalid_recovery_phrase_1)
            AccountRecoveryStatus.WORD_COUNT_ERROR ->
                showSeedPhraseInputError(R.string.recovery_phrase_word_count_error)
            AccountRecoveryStatus.RECOVERY_SUCCESSFUL -> {
                if (email.isNotEmpty()) {
                    // Go to the reset password screen when we have an email
                    launchResetPasswordFlow(newState.seedPhrase)
                } else {
                    // Launch pin-entry otherwise
                    start<PinEntryActivity>(this)
                }
            }
            AccountRecoveryStatus.RECOVERY_FAILED ->
                toast(R.string.restore_failed, ToastCustom.TYPE_ERROR)
            AccountRecoveryStatus.RESET_KYC_FAILED ->
                toast(R.string.reset_kyc_failed, ToastCustom.TYPE_ERROR)
            else -> {
                // Do nothing.
            }
        }
        binding.progressBar.visibleIf { isRecoveryInProgress(newState) }
    }

    private fun isRecoveryInProgress(newState: AccountRecoveryState) =
        newState.status == AccountRecoveryStatus.VERIFYING_SEED_PHRASE ||
            newState.status == AccountRecoveryStatus.RECOVERING_CREDENTIALS ||
            newState.status == AccountRecoveryStatus.RESETTING_KYC

    private fun initControls() {
        with(binding) {
            backButton.setOnClickListener { finish() }
            recoveryPhaseText.apply {
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {
                        binding.recoveryPhaseTextLayout.apply {
                            isErrorEnabled = false
                            error = ""
                        }
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
            resetAccountLabel.apply {
                visibleIf { email.isNotEmpty() }
                text = StringUtils.getStringWithMappedAnnotations(
                    context = this@AccountRecoveryActivity,
                    stringId = R.string.reset_account_notice,
                    linksMap = emptyMap(),
                    onClick = { launchResetAccountFlow() }
                )
                movementMethod = LinkMovementMethod.getInstance()
            }
            resetKycLabel.text = getString(R.string.reset_kyc_notice)

            verifyButton.setOnClickListener {
                model.process(
                    AccountRecoveryIntents.VerifySeedPhrase(
                        seedPhrase = recoveryPhaseText.text?.toString() ?: ""
                    )
                )
            }
        }
    }

    private fun launchResetAccountFlow() {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(
                binding.fragmentContainer.id,
                ResetAccountFragment.newInstance(
                    email = email,
                    recoveryToken = intent.getStringExtra(ResetPasswordFragment.RECOVERY_TOKEN) ?: ""
                ),
                ResetAccountFragment::class.simpleName
            )
            .addToBackStack(ResetAccountFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    private fun launchResetPasswordFlow(recoveryPhrase: String) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(
                binding.fragmentContainer.id,
                ResetPasswordFragment.newInstance(
                    isResetMandatory = false,
                    email = email,
                    recoveryPhrase = recoveryPhrase
                ),
                ResetPasswordFragment::class.simpleName
            )
            .addToBackStack(ResetPasswordFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    private fun showSeedPhraseInputError(@StringRes errorText: Int) {
        binding.recoveryPhaseTextLayout.apply {
            isErrorEnabled = true
            error = getString(errorText)
        }
    }
}