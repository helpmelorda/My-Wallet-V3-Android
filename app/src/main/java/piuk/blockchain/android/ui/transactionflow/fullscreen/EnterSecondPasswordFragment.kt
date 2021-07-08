package piuk.blockchain.android.ui.transactionflow.fullscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentTxFlowPasswordBinding
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import timber.log.Timber

class EnterSecondPasswordFragment : TransactionFlowFragment<FragmentTxFlowPasswordBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTxFlowPasswordBinding =
        FragmentTxFlowPasswordBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            ctaButton.setOnClickListener { onCtaClick() }
            passwordInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    onCtaClick()
                }
                true
            }
        }
    }

    override fun render(newState: TransactionState) {
        Timber.d("!TRANSACTION!> Rendering! EnterSecondPasswordFragment")
        require(newState.currentStep == TransactionStep.ENTER_PASSWORD)

        if (newState.errorState == TransactionErrorState.INVALID_PASSWORD) {
            Toast.makeText(requireContext(), getString(R.string.invalid_password), Toast.LENGTH_SHORT).show()
        }
    }

    private fun onCtaClick() {
        model.process(TransactionIntent.ValidatePassword(binding.passwordInput.text.toString()))
    }

    companion object {
        fun newInstance(): EnterSecondPasswordFragment = EnterSecondPasswordFragment()
    }
}
