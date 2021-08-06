package piuk.blockchain.android.ui.transactionflow

import android.app.Activity
import androidx.fragment.app.FragmentManager
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.ui.transactionflow.fullscreen.TransactionFlowActivity

class TransactionLauncher(private val flags: InternalFeatureFlagApi) {

    fun startFlow(
        activity: Activity,
        fragmentManager: FragmentManager,
        flowHost: DialogFlow.FlowHost,
        action: AssetAction,
        sourceAccount: BlockchainAccount = NullCryptoAccount(),
        target: TransactionTarget = NullCryptoAccount()
    ) {
        if (flags.isFeatureEnabled(GatedFeature.FULL_SCREEN_TXS)) {
            activity.startActivity(TransactionFlowActivity.newInstance(activity, sourceAccount, target, action))
        } else {
            TransactionFlow(sourceAccount, target, action).also {
                it.startFlow(fragmentManager, flowHost)
            }
        }
    }
}