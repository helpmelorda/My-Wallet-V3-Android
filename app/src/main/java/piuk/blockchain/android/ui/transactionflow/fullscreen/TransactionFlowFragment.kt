package piuk.blockchain.android.ui.transactionflow.fullscreen

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.viewbinding.ViewBinding
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.transactionInject

abstract class TransactionFlowFragment<T : ViewBinding> :
    MviFragment<TransactionModel, TransactionIntent, TransactionState, T>() {

    override val model: TransactionModel by transactionInject()

    protected var state: TransactionState = TransactionState()
        private set

    protected val analyticsHooks: TxFlowAnalytics by inject()

    abstract fun getActionName(): String

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedInstanceState?.let {
            model.process(TransactionIntent.ResetFlow)
        }
    }

    protected fun showErrorToast(@StringRes msgId: Int) {
        ToastCustom.makeText(
            activity,
            getString(msgId),
            ToastCustom.LENGTH_LONG,
            ToastCustom.TYPE_ERROR
        )
    }

    protected fun cacheState(newState: TransactionState) {
        state = newState
    }
}