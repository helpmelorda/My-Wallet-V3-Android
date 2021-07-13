package piuk.blockchain.android.data.stores

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.`should be equal to`
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.androidcore.data.datastores.persistentstore.DefaultFetchStrategy
import com.blockchain.data.datastores.PersistentStore
import com.blockchain.utils.Optional

class DefaultFetchStrategyTest : RxTest() {

    lateinit var subject: DefaultFetchStrategy<String>
    lateinit var webSource: Observable<String>
    lateinit var memorySource: Observable<Optional<String>>
    val memoryStore: PersistentStore<String> = mock()

    @Test
    fun `fetch should call web source`() {
        // Arrange
        val value = "VALUE"
        memorySource = Observable.just(Optional.None)
        webSource = Observable.just(value)
        whenever(memoryStore.store(value)).thenReturn(Observable.just(value))
        subject = DefaultFetchStrategy(
            webSource,
            memorySource,
            memoryStore
        )
        // Act
        val testObserver = subject.fetch().test()
        // Assert
        verify(memoryStore).store(value)
        testObserver.values()[0] `should be equal to` value
    }

    @Test
    fun `fetch should return memory source and not call web source`() {
        // Arrange
        val value = "VALUE"
        memorySource = Observable.just(Optional.Some(value))
        webSource = Observable.error { AssertionError("This should not be called") }
        whenever(memoryStore.store(value)).thenReturn(Observable.just(value))
        subject = DefaultFetchStrategy(
            webSource,
            memorySource,
            memoryStore
        )
        // Act
        val testObserver = subject.fetch().test()
        // Assert
        verifyZeroInteractions(memoryStore)
        testObserver.values()[0] `should be equal to` value
    }
}