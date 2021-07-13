package piuk.blockchain.android.ui.start

import com.blockchain.nabu.datamanagers.ApiStatus
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single

import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.PersistentPrefs

class LandingPresenterTest {

    private lateinit var subject: LandingPresenter
    private val view: LandingView = mock()
    private val apiStatus: ApiStatus = mock {
        on { isHealthy() }.thenReturn(Single.just(true))
    }
    private val environmentSettings: EnvironmentConfig =
        mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)

    private val prefs: PersistentPrefs = mock()
    private val rootUtil: RootUtil = mock()

    @Before
    fun setUp() {
        subject = LandingPresenter(environmentSettings, prefs, rootUtil, apiStatus)
    }

    @Test
    fun `onViewReady no debug`() {
        // Arrange
        whenever(environmentSettings.isRunningInDebugMode()).thenReturn(false)
        // Act
        subject.attachView(view)
        // Assert
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `device is rooted and warnings are off - don't show dialog`() {
        // Arrange
        whenever(rootUtil.isDeviceRooted).thenReturn(true)
        whenever(prefs.disableRootedWarning).thenReturn(false)

        subject.attachView(view)

        // Act
        subject.checkForRooted()

        // Assert
        verify(view).showIsRootedWarning()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `device is not rooted and warnings are off - don't show dialog`() {
        // Arrange
        whenever(rootUtil.isDeviceRooted).thenReturn(false)
        whenever(prefs.disableRootedWarning).thenReturn(false)

        subject.attachView(view)

        // Act
        subject.checkForRooted()

        // Assert
        verifyZeroInteractions(view)
    }

    @Test
    fun `device is rooted and warnings are on - show dialog`() {
        // Arrange
        whenever(rootUtil.isDeviceRooted).thenReturn(true)
        whenever(prefs.disableRootedWarning).thenReturn(false)

        subject.attachView(view)

        // Act
        subject.checkForRooted()

        // Assert
        verify(view).showIsRootedWarning()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `device is not rooted and warnings are on - don't show dialog`() {
        // Arrange
        whenever(rootUtil.isDeviceRooted).thenReturn(false)
        whenever(prefs.disableRootedWarning).thenReturn(true)

        // Act
        subject.checkForRooted()

        // Assert
        verifyZeroInteractions(view)
    }
}
