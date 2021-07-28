package piuk.blockchain.android.ui.transactionflow.fullscreen

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.viewbinding.ViewBinding
import org.koin.android.ext.android.inject
import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState

abstract class TransactionFlowFragment<T : ViewBinding> :
    MviFragment<TransactionModel, TransactionIntent, TransactionState, T>() {

    private val scope: Scope by lazy {
        KoinJavaComponent.getKoin().getScope(TransactionFlowActivity.TX_SCOPE_ID)
    }

    override val model: TransactionModel by scope.inject()

    protected val analyticsHooks: TxFlowAnalytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}