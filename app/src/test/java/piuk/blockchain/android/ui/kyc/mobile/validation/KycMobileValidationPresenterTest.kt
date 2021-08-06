package piuk.blockchain.android.ui.kyc.mobile.validation

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.NabuUserSync
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.kyc.mobile.entry.models.PhoneVerificationModel
import piuk.blockchain.android.ui.kyc.mobile.validation.models.VerificationCode
import piuk.blockchain.androidcore.data.settings.PhoneNumber
import piuk.blockchain.androidcore.data.settings.PhoneNumberUpdater

class KycMobileValidationPresenterTest {

    private lateinit var subject: KycMobileValidationPresenter
    private val view: KycMobileValidationView = mock()
    private val phoneNumberUpdater: PhoneNumberUpdater = mock()
    private val nabuUserSync: NabuUserSync = mock {
        on { syncUser() }.thenReturn(Completable.complete())
    }

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycMobileValidationPresenter(
            nabuUserSync,
            phoneNumberUpdater,
            mock()
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady, should progress page`() {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val verificationCode = VerificationCode("VERIFICATION_CODE")
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(view.resendObservable).thenReturn(noResend())
        whenever(phoneNumberUpdater.verifySms(verificationCode.code))
            .thenReturn(Single.just(phoneNumberSanitized))
        // Act
        subject.onViewReady()
        publishSubject.onNext(
            PhoneVerificationModel(
                phoneNumberSanitized,
                verificationCode
            ) to Unit
        )
        // Assert
        verify(nabuUserSync).syncUser()
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueSignUp()
    }

    @Test
    fun `on resend`() {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        val resendSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(view.resendObservable).thenReturn(resendSubject)
        whenever(phoneNumberUpdater.updateSms(any()))
            .thenReturn(Single.just(phoneNumberSanitized))
        // Act
        subject.onViewReady()
        resendSubject.onNext(
            PhoneNumber(
                phoneNumberSanitized
            ) to Unit
        )
        // Assert
        verify(phoneNumberUpdater).updateSms(argThat { sanitized == phoneNumberSanitized })
        verify(view).theCodeWasResent()
        verify(nabuUserSync).syncUser()
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view, never()).continueSignUp()
    }

    @Test
    fun `onViewReady, should throw exception and resubscribe for next event`() {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val verificationCode = VerificationCode("VERIFICATION_CODE")
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(view.resendObservable).thenReturn(noResend())
        whenever(phoneNumberUpdater.verifySms(verificationCode.code))
            .thenReturn(Single.just(phoneNumberSanitized))
        whenever(nabuUserSync.syncUser())
            .thenReturn(Completable.error { Throwable() })
            .thenReturn(Completable.complete())
        val verificationModel = PhoneVerificationModel(phoneNumberSanitized, verificationCode)

        // Act
        subject.onViewReady()
        publishSubject.onNext(verificationModel to Unit)
        publishSubject.onNext(verificationModel to Unit)
        // Assert
        verify(view, times(2)).showProgressDialog()
        verify(view, times(2)).dismissProgressDialog()
        verify(nabuUserSync, times(2)).syncUser()
        verify(view).displayErrorDialog(any())
        verify(view).continueSignUp()
    }

    @Test
    fun `onViewReady, should throw exception and display error dialog`() {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val verificationCode = VerificationCode("VERIFICATION_CODE")
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(view.resendObservable).thenReturn(noResend())
        whenever(phoneNumberUpdater.verifySms(verificationCode.code))
            .thenReturn(Single.just(phoneNumberSanitized))
        whenever(nabuUserSync.syncUser())
            .thenReturn(Completable.error { Throwable() })
        // Act
        subject.onViewReady()
        publishSubject.onNext(
            PhoneVerificationModel(
                phoneNumberSanitized,
                verificationCode
            ) to Unit
        )
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).displayErrorDialog(any())
        verify(nabuUserSync).syncUser()
    }
}

private fun noResend(): Observable<Pair<PhoneNumber, Unit>> = Observable.never()
