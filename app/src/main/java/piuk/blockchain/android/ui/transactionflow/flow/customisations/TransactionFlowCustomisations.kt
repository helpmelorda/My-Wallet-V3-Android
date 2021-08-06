package piuk.blockchain.android.ui.transactionflow.flow.customisations

import piuk.blockchain.android.ui.transactionflow.engine.TransactionState

interface TransactionFlowCustomisations {
    fun getScreenTitle(state: TransactionState): String
    fun getBackNavigationAction(state: TransactionState): BackNavigationState
}

sealed class BackNavigationState {
    object ResetPendingTransaction : BackNavigationState()
    object ClearTransactionTarget : BackNavigationState()
    object NavigateToPreviousScreen : BackNavigationState()
}