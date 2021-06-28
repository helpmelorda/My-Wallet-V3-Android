package piuk.blockchain.android.ui.kyc

import com.blockchain.nabu.NabuToken
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

abstract class BaseKycPresenter<T : View>(
    private val nabuToken: NabuToken
) : BasePresenter<T>() {

    protected val fetchOfflineToken by unsafeLazy { nabuToken.fetchNabuToken() }
}
