package piuk.blockchain.android.ui.transactionflow.fullscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.databinding.FragmentTxAccountSelectorBinding
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TargetSelectionCustomisations
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class SelectTargetAccountFragment : TransactionFlowFragment<FragmentTxAccountSelectorBinding>() {

    private val customiser: TargetSelectionCustomisations by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            accountList.onListLoaded = ::doOnListLoaded
            accountList.onLoadError = ::doOnLoadError
        }
    }

    override fun render(newState: TransactionState) {
        with(binding) {
            accountList.initialise(
                source = Single.just(newState.availableTargets.map { it as SingleAccount }),
                status = customiser.selectTargetStatusDecorator(newState)
            )
            accountListSubtitle.text = customiser.selectTargetAccountDescription(newState)
            accountListSubtitle.visible()
            accountList.onAccountSelected = { account: BlockchainAccount -> doOnAccountSelected(account, newState) }
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTxAccountSelectorBinding =
        FragmentTxAccountSelectorBinding.inflate(inflater, container, false)

    private fun doOnListLoaded(isEmpty: Boolean) {
        binding.progress.gone()
    }

    private fun doOnAccountSelected(account: BlockchainAccount, state: TransactionState) {
        require(account is SingleAccount)
        model.process(TransactionIntent.TargetAccountSelected(account))
        analyticsHooks.onTargetAccountSelected(account, state)
    }

    private fun doOnLoadError(it: Throwable) {
        binding.accountListEmpty.visible()
        binding.progress.gone()
    }

    companion object {
        fun newInstance(): SelectTargetAccountFragment = SelectTargetAccountFragment()
    }
}