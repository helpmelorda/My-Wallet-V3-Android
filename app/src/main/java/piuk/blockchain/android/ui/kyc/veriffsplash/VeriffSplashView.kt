package piuk.blockchain.android.ui.kyc.veriffsplash

import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.veriff.VeriffApplicantAndToken
import io.reactivex.rxjava3.core.Observable
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.kyc.splash.UiState

interface VeriffSplashView : View {

    val countryCode: String

    val nextClick: Observable<Unit>

    val swapClick: Observable<Unit>

    fun continueToVeriff(applicant: VeriffApplicantAndToken)

    fun continueToCompletion()

    fun continueToSwap()

    fun supportedDocuments(documents: List<SupportedDocuments>)

    fun setUiState(@UiState.UiStateDef state: Int)

    fun showError(message: Int)

    fun showProgressDialog(cancelable: Boolean)

    fun dismissProgressDialog()
}