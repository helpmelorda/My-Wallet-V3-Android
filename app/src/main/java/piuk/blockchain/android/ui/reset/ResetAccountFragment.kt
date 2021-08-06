package piuk.blockchain.android.ui.reset

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentAccountResetBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment

class ResetAccountFragment :
    MviFragment<ResetAccountModel, ResetAccountIntents, ResetAccountState, FragmentAccountResetBinding>() {

    override val model: ResetAccountModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAccountResetBinding =
        FragmentAccountResetBinding.inflate(inflater, container, false)

    override fun render(newState: ResetAccountState) {
        when (newState.status) {
            ResetAccountStatus.SHOW_INFO -> showInfoScreen()
            ResetAccountStatus.SHOW_WARNING -> showWarningScreen()
            ResetAccountStatus.RETRY -> onBackPressed()
            ResetAccountStatus.RESET -> {
                // TODO navigate to next screen
            }
        }
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
}