package piuk.blockchain.android.ui.reset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentAccountResetBinding
import piuk.blockchain.android.ui.base.addAnimationTransaction
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.reset.password.ResetPasswordFragment

class ResetAccountFragment :
    MviFragment<ResetAccountModel, ResetAccountIntents, ResetAccountState, FragmentAccountResetBinding>() {

    override val model: ResetAccountModel by scopedInject()

    private var isInitialLoop = false

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAccountResetBinding =
        FragmentAccountResetBinding.inflate(inflater, container, false)

    override fun render(newState: ResetAccountState) {
        when (newState.status) {
            ResetAccountStatus.SHOW_INFO -> showInfoScreen()
            ResetAccountStatus.SHOW_WARNING -> showWarningScreen()
            ResetAccountStatus.RETRY -> onBackPressed()
            ResetAccountStatus.RESET -> {
                if (isInitialLoop) {
                    // To handle navigating back to this screen.
                    model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.SHOW_WARNING))
                } else {
                    launchResetPasswordFlow()
                }
            }
        }

        if (isInitialLoop) {
            isInitialLoop = false
        }
    }

    override fun onStart() {
        super.onStart()
        isInitialLoop = true
    }

    private fun showInfoScreen() {
        with(binding) {
            resetImage.setImageResource(R.drawable.ic_reset_round)
            resetAccountLabel.text = getString(R.string.reset_account_title)
            resetAccountDesc.text = getString(R.string.reset_account_description)
            resetButton.apply {
                text = getString(R.string.reset_account_cta)
                setOnClickListener {
                    model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.SHOW_WARNING))
                }
            }
            retryButton.apply {
                text = getString(R.string.retry_recovery_phrase_cta)
                setOnClickListener {
                    model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.RETRY))
                }
            }
            backButton.setOnClickListener { onBackPressed() }
        }
    }

    private fun showWarningScreen() {
        with(binding) {
            resetImage.setImageResource(R.drawable.ic_triangle_warning_circle)
            resetAccountLabel.text = getString(R.string.reset_account_warning_title)
            resetAccountDesc.text = getString(R.string.reset_account_warning_description)
            resetButton.apply {
                text = getString(R.string.reset_account_cta)
                setOnClickListener {
                    model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.RESET))
                }
            }
            retryButton.apply {
                text = getString(R.string.common_go_back)
                setOnClickListener {
                    model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.SHOW_INFO))
                }
            }
            backButton.setOnClickListener {
                model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.SHOW_INFO))
            }
        }
    }

    private fun onBackPressed() = parentFragmentManager.popBackStack()

    private fun launchResetPasswordFlow() {
        parentFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(
                binding.fragmentContainer.id,
                ResetPasswordFragment.newInstance(
                    isResetMandatory = true,
                    email = arguments?.getString(ResetPasswordFragment.EMAIL) ?: "",
                    recoveryToken = arguments?.getString(ResetPasswordFragment.RECOVERY_TOKEN) ?: ""
                ),
                ResetPasswordFragment::class.simpleName
            )
            .addToBackStack(ResetPasswordFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    companion object {
        fun newInstance(email: String, recoveryToken: String): ResetAccountFragment {
            return ResetAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(ResetPasswordFragment.EMAIL, email)
                    putString(ResetPasswordFragment.RECOVERY_TOKEN, recoveryToken)
                }
            }
        }
    }
}