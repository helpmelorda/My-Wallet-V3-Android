package piuk.blockchain.android.ui.upsell

import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class KycUpgradePromptManager(
    private val identity: UserIdentity
) {
    fun queryUpsell(action: AssetAction, account: BlockchainAccount): Single<Type> =
        when (action) {
            AssetAction.Receive -> checkReceiveUpsell(account)
            else -> Single.just(Type.NONE)
        }

    private fun checkReceiveUpsell(account: BlockchainAccount): Single<Type> =
        identity.isVerifiedFor(Feature.TierLevel(Tier.SILVER))
            .map { isSilver ->
                Type.CUSTODIAL_RECEIVE.takeIf { !isSilver && account is CustodialTradingAccount } ?: Type.NONE
            }

    enum class Type {
        NONE,
        CUSTODIAL_RECEIVE
    }

    companion object {
        fun getUpsellSheet(upsellType: Type): SlidingModalBottomDialog<*> =
            when (upsellType) {
                Type.NONE -> throw IllegalArgumentException("Cannot create upsell sheet of type NONE")
                Type.CUSTODIAL_RECEIVE -> CustodialReceiveUpsellSheet()
            }
    }
}
