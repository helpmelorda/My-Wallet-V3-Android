package piuk.blockchain.android.ui.transactionflow.fullscreen

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogTxFlowInProgressBinding
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import piuk.blockchain.android.ui.transactionflow.engine.TxExecutionStatus
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionProgressCustomisations
import timber.log.Timber

class TransactionProgressFragment : TransactionFlowFragment<DialogTxFlowInProgressBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogTxFlowInProgressBinding =
        DialogTxFlowInProgressBinding.inflate(inflater, container, false)

    private val customiser: TransactionProgressCustomisations by inject()
    private val MAX_STACKTRACE_CHARS = 400

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity.supportActionBar?.hide()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.txProgressView.onCtaClick {
            activity.finish()
        }

        // this is needed to show the expanded dialog, with space at the top and bottom
        val metrics = DisplayMetrics()
        requireActivity().windowManager?.defaultDisplay?.getMetrics(metrics)
        binding.root.layoutParams.height = (metrics.heightPixels - (48 * metrics.density)).toInt()
        binding.root.requestLayout()
    }

    override fun render(newState: TransactionState) {
        Timber.d("!TRANSACTION!> Rendering! TransactionProgressSheet")
        require(newState.currentStep == TransactionStep.IN_PROGRESS)

        customiser.transactionProgressStandardIcon(newState)?.let {
            binding.txProgressView.setAssetIcon(it)
        } ?: binding.txProgressView.setAssetIcon(newState.sendingAsset)

        handleStatusUpdates(newState)
        cacheState(newState)
    }

    private fun handleStatusUpdates(
        newState: TransactionState
    ) {
        when (newState.executionStatus) {
            is TxExecutionStatus.InProgress -> binding.txProgressView.showTxInProgress(
                customiser.transactionProgressTitle(newState),
                customiser.transactionProgressMessage(newState)
            )
            is TxExecutionStatus.Completed -> {
                analyticsHooks.onTransactionSuccess(newState)
                binding.txProgressView.showTxSuccess(
                    customiser.transactionCompleteTitle(newState),
                    customiser.transactionCompleteMessage(newState)
                )
            }
            is TxExecutionStatus.ApprovalRequired -> {
                binding.txProgressView.showTxInProgress(
                    customiser.transactionProgressTitle(newState),
                    customiser.transactionProgressMessage(newState)
                )
                startActivity(
                    BankAuthActivity.newInstance(
                        newState.executionStatus.approvalData, BankAuthSource.DEPOSIT, requireContext()
                    )
                )
                // dismiss()
            }
            is TxExecutionStatus.Error -> {
                analyticsHooks.onTransactionFailure(
                    newState, collectStackTraceString(newState.executionStatus.exception)
                )
                binding.txProgressView.showTxError(
                    customiser.transactionProgressExceptionMessage(newState),
                    getString(R.string.send_progress_error_subtitle)
                )
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun collectStackTraceString(e: Throwable): String {
        var stackTraceString = ""
        var index = 0
        while (stackTraceString.length <= MAX_STACKTRACE_CHARS && index < e.stackTrace.size) {
            stackTraceString += "${e.stackTrace[index]}\n"
            index++
        }
        Timber.d("Sending trace to analytics: $stackTraceString")
        return stackTraceString
    }

    companion object {
        fun newInstance(): TransactionProgressFragment = TransactionProgressFragment()
    }
}