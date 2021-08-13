package piuk.blockchain.android.ui.reset.password

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import com.blockchain.wallet.DefaultLabels
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentPasswordResetBinding
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.mvi.MviActivity.Companion.start
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.urllinks.URL_PRIVACY_POLICY
import piuk.blockchain.android.urllinks.URL_TOS_POLICY
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

class ResetPasswordFragment :
    MviFragment<ResetPasswordModel, ResetPasswordIntents, ResetPasswordState, FragmentPasswordResetBinding>() {

    private val defaultLabels: DefaultLabels by inject()

    private val isPasswordResetMandatory: Boolean by lazy {
        arguments?.getBoolean(IS_MANDATORY, false) ?: false
    }

    private val email: String by lazy {
        arguments?.getString(EMAIL) ?: ""
    }

    private val recoveryToken: String by lazy {
        arguments?.getString(RECOVERY_TOKEN) ?: ""
    }

    private val recoveryPhrase: String by lazy {
        arguments?.getString(SEED_PHRASE) ?: ""
    }

    private val shouldRecoverWallet: Boolean by lazy {
        recoveryPhrase.isNotBlank() && email.isNotBlank()
    }

    private val shouldRecoverAccount: Boolean by lazy {
        recoveryToken.isNotBlank() && email.isNotBlank()
    }

    override val model: ResetPasswordModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPasswordResetBinding =
        FragmentPasswordResetBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            initUI(isPasswordResetMandatory)
            initControls(isPasswordResetMandatory)
        }
    }

    override fun render(newState: ResetPasswordState) {
        when (newState.status) {
            ResetPasswordStatus.SHOW_ERROR -> {
                binding.progressBar.gone()
                toast(getString(R.string.common_error), ToastCustom.TYPE_ERROR)
            }
            ResetPasswordStatus.SHOW_SUCCESS -> {
                binding.progressBar.gone()
                start<PinEntryActivity>(requireContext())
            }
            ResetPasswordStatus.CREATE_WALLET,
            ResetPasswordStatus.RECOVER_ACCOUNT,
            ResetPasswordStatus.RESTORE_WALLET,
            ResetPasswordStatus.RESET_KYC -> binding.progressBar.visible()
            else -> {
                binding.progressBar.gone()
            }
        }
    }

    private fun processPassword(password: String) {
        model.process(
            when {
                shouldRecoverWallet ->
                    ResetPasswordIntents.RecoverWallet(
                        email = email,
                        password = password,
                        recoveryPhrase = recoveryPhrase,
                        walletName = defaultLabels.getDefaultNonCustodialWalletLabel(),
                        shouldResetKyc = isPasswordResetMandatory
                    )
                shouldRecoverAccount ->
                    ResetPasswordIntents.CreateWalletForAccount(
                        email = email,
                        password = password,
                        recoveryToken = recoveryToken,
                        walletName = defaultLabels.getDefaultNonCustodialWalletLabel(),
                        shouldResetKyc = isPasswordResetMandatory
                    )
                else -> ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR)
            }
        )
    }

    private fun FragmentPasswordResetBinding.initControls(isMandatory: Boolean) {
        backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        skipButton.apply {
            visibleIf { !isMandatory }
            setOnClickListener { start<PinEntryActivity>(requireContext()) }
        }

        continueButton.setOnClickListener {
            if (passwordView.isPasswordValid()) {
                processPassword(passwordView.getEnteredPassword())
            }
        }
    }

    private fun FragmentPasswordResetBinding.initUI(isMandatory: Boolean) {
        resetPasswordTitle.text = if (isMandatory) {
            getString(R.string.common_reset_password)
        } else {
            getString(R.string.reset_password_optional_title)
        }
        optionalResetPasswordLabel.visibleIf { !isMandatory }
        val linksMap = mapOf<String, Uri>(
            "terms" to Uri.parse(URL_TOS_POLICY),
            "privacy" to Uri.parse(URL_PRIVACY_POLICY)
        )
        privacyNotice.apply {
            text = StringUtils.getStringWithMappedAnnotations(
                context = requireContext(),
                stringId = R.string.you_agree_terms_of_service,
                linksMap = linksMap
            )
            movementMethod = LinkMovementMethod.getInstance()
            visibleIf { isMandatory }
        }
        resetKycNotice.apply {
            text = getString(R.string.reset_kyc_notice)
            visibleIf { isMandatory }
        }
        continueButton.text = if (!isMandatory) {
            getString(R.string.common_continue)
        } else {
            getString(R.string.common_reset_password)
        }
    }

    companion object {
        const val RECOVERY_TOKEN = "recovery_token"
        const val EMAIL = "email"
        const val SEED_PHRASE = "seed_phrase"
        private const val IS_MANDATORY = "is_mandatory"

        fun newInstance(
            isResetMandatory: Boolean,
            email: String = "",
            recoveryPhrase: String = "",
            recoveryToken: String = ""
        ): ResetPasswordFragment {
            return ResetPasswordFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_MANDATORY, isResetMandatory)
                    putString(EMAIL, email)
                    putString(SEED_PHRASE, recoveryPhrase)
                    putString(RECOVERY_TOKEN, recoveryToken)
                }
            }
        }
    }
}