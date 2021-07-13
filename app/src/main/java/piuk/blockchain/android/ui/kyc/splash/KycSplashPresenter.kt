package piuk.blockchain.android.ui.kyc.splash

import piuk.blockchain.android.ui.kyc.BaseKycPresenter
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import com.blockchain.nabu.NabuToken
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class KycSplashPresenter(
    nabuToken: NabuToken,
    private val kycNavigator: KycNavigator
) : BaseKycPresenter<KycSplashView>(nabuToken) {

    override fun onViewReady() {}

    fun onCTATapped() {
        goToNextKycStep()
    }

    private fun goToNextKycStep() {
        compositeDisposable += kycNavigator.findNextStep()
            .subscribeBy(
                onError = { Timber.e(it) },
                onSuccess = { view.goToNextKycStep(it) }
            )
    }
}