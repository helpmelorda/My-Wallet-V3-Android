package piuk.blockchain.androidcore.data.settings.datastore

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import com.blockchain.utils.Optional

class SettingsDataStoreTest : RxTest() {

    private lateinit var subject: SettingsDataStore
    private lateinit var webSource: Observable<Settings>
    private val memoryStore: SettingsMemoryStore = mock()

    @Test
    fun `getSettings using DefaultFetchStrategy from websource`() {
        // Arrange
        val mockSettings: Settings = mock()
        webSource = Observable.just(mockSettings)
        whenever(memoryStore.getSettings()).thenReturn(Observable.just(Optional.None))
        whenever(memoryStore.store(mockSettings)).thenReturn(Observable.just(mockSettings))
        subject = SettingsDataStore(memoryStore, webSource)
        // Act
        val testObserver = subject.getSettings().test()
        // Assert
        verify(memoryStore).getSettings()
        testObserver.assertValue { it == mockSettings }
    }

    @Test
    fun fetchSettings() {
        // Arrange
        val mockSettings: Settings = mock()
        webSource = Observable.just(mockSettings)
        whenever(memoryStore.store(mockSettings)).thenReturn(Observable.just(mockSettings))
        subject = SettingsDataStore(memoryStore, webSource)
        // Act
        val testObserver = subject.fetchSettings().test()
        // Assert
        verify(memoryStore).store(mockSettings)
        testObserver.assertValue { it == mockSettings }
    }
}