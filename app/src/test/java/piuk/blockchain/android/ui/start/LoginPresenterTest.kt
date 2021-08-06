package piuk.blockchain.android.ui.start

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.rxjava3.core.Completable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import javax.net.ssl.SSLPeerUnverifiedException

class LoginPresenterTest {

    private lateinit var subject: LoginPresenter
    private val view: LoginView = mock()
    private val appUtil: AppUtil = mock()
    private val _payloadDataManager: Lazy<PayloadDataManager> = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val prefsUtil: PersistentPrefs = mock()
    private val analytics: Analytics = mock()

    @Before
    fun setUp() {
        subject = LoginPresenter(appUtil, _payloadDataManager, prefsUtil, analytics)
        whenever(_payloadDataManager.value).thenReturn(payloadDataManager)
    }

    @Test
    fun `pairWithQR success`() {
        // Arrange
        val qrCode = "QR_CODE"
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        whenever(payloadDataManager.handleQrCode(qrCode)).thenReturn(Completable.complete())
        whenever(payloadDataManager.wallet).thenReturn(Wallet().apply {
            this.sharedKey = sharedKey
            this.guid = guid
        })
        subject.attachView(view)

        // Act
        subject.pairWithQR(qrCode)

        // Assert
        verify(view).showProgressDialog(R.string.please_wait, null)
        verify(view).dismissProgressDialog()
        verify(view).startPinEntryActivity()
        verifyNoMoreInteractions(view)
        verifyNoMoreInteractions(appUtil)
        verify(prefsUtil).sharedKey = sharedKey
        verify(prefsUtil).walletGuid = guid
        verify(prefsUtil).setValue(PersistentPrefs.KEY_EMAIL_VERIFIED, true)
        verify(prefsUtil).setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, true)
        verify(analytics).logEvent(AnalyticsEvents.WalletAutoPairing)
        verifyNoMoreInteractions(prefsUtil)
        verify(payloadDataManager).handleQrCode(qrCode)
        verify(payloadDataManager, atLeastOnce()).wallet
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun `pairWithQR fail`() {
        // Arrange
        val qrCode = "QR_CODE"
        whenever(payloadDataManager.handleQrCode(qrCode)).thenReturn(Completable.error(Throwable()))
        subject.attachView(view)

        // Act
        subject.pairWithQR(qrCode)

        // Assert
        verify(view).showProgressDialog(R.string.please_wait, null)
        verify(view).dismissProgressDialog()
        //noinspection WrongConstant
        verify(view).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(view)
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity::class.java)
        verifyNoMoreInteractions(appUtil)
        verifyZeroInteractions(prefsUtil)
        verify(analytics, never()).logEvent(AnalyticsEvents.WalletAutoPairing)
        verify(payloadDataManager).handleQrCode(qrCode)
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun `pairWithQR SSL Exception`() {
        // Arrange
        val qrCode = "QR_CODE"
        whenever(payloadDataManager.handleQrCode(qrCode)).thenReturn(
            Completable.error(
                SSLPeerUnverifiedException("")
            )
        )
        subject.attachView(view)

        // Act
        subject.pairWithQR(qrCode)

        // Assert
        verify(view).showProgressDialog(R.string.please_wait, null)
        verify(view).dismissProgressDialog()
        verifyNoMoreInteractions(view)
        verify(appUtil).clearCredentials()
        verifyNoMoreInteractions(appUtil)
        verifyZeroInteractions(prefsUtil)
        verify(payloadDataManager).handleQrCode(qrCode)
        verifyNoMoreInteractions(payloadDataManager)
    }
}