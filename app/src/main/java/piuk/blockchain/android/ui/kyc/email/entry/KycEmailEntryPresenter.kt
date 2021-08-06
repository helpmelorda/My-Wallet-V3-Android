package piuk.blockchain.android.ui.kyc.email.entry

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import timber.log.Timber

class KycEmailEntryPresenter(
    private val emailUpdater: EmailSyncUpdater
) : BasePresenter<KycEmailEntryView>() {

    override fun onViewReady() {
        preFillEmail()
        subscribeToClickEvents()
    }

    private fun subscribeToClickEvents() {
        compositeDisposable +=
            view.uiStateObservable
                .map { it.first }
                .flatMapCompletable { email ->
                    emailUpdater.updateEmailAndSync(email)
                        .ignoreElement()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate { view.dismissProgressDialog() }
                        .doOnError {
                            Timber.e(it)
                            view.showError(R.string.kyc_email_error_saving_email)
                        }
                        .doOnComplete {
                            view.continueSignUp(email)
                        }
                }
                .retry()
                .doOnError(Timber::e)
                .subscribe()
    }

    private fun preFillEmail() {
        compositeDisposable +=
            emailUpdater.email()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        view.preFillEmail(it.address)
                    },
                    // Ignore error
                    onError = { Timber.d(it) }
                )
    }

    internal fun onProgressCancelled() {
        // Cancel outbound
        compositeDisposable.clear()
        // Resubscribe to everything
        subscribeToClickEvents()
    }

    fun dispose() {
        compositeDisposable.clear()
    }
}
