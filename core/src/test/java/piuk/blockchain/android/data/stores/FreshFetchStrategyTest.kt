package piuk.blockchain.android.data.stores

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.`should be equal to`
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.androidcore.data.datastores.persistentstore.FreshFetchStrategy
import com.blockchain.data.datastores.PersistentStore

class FreshFetchStrategyTest : RxTest() {

    lateinit var subject: FreshFetchStrategy<String>
    lateinit var webSource: Observable<String>
    val memoryStore: PersistentStore<String> = mock()

    @Test
    fun `fetch should store in memory`() {
        val value = "VALUE"
        webSource = Observable.just(value)
        whenever(memoryStore.store(value)).thenReturn(Observable.just(value))
        subject = FreshFetchStrategy(
            webSource,
            memoryStore
        )
        // Act
        val testObserver = subject.fetch().test()
        // Assert
        verify(memoryStore).store(value)
        testObserver.values()[0] `should be equal to` value
    }
}