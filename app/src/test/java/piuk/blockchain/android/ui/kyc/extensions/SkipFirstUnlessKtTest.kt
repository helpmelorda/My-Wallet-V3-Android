package piuk.blockchain.android.ui.kyc.extensions

import io.reactivex.rxjava3.kotlin.toObservable
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class SkipFirstUnlessKtTest {

    @Test
    fun `should filter out initial empty string`() {
        val source = listOf("", "one", "two", "three", "four").toObservable()
        val testObserver = source.skipFirstUnless { !it.isEmpty() }.test()
        val list = testObserver.values()
        list `should be equal to` listOf("one", "two", "three", "four")
    }

    @Test
    fun `should not filter out second item matching condition`() {
        val source = listOf("one", "", "two", "three", "four").toObservable()
        val testObserver = source.skipFirstUnless { !it.isEmpty() }.test()
        val list = testObserver.values()
        list `should be equal to` listOf("one", "", "two", "three", "four")
    }

    @Test
    fun `should only filter initially matching item`() {
        val source = listOf("", "", "", "", "").toObservable()
        val testObserver = source.skipFirstUnless { !it.isEmpty() }.test()
        val list = testObserver.values()
        list `should be equal to` listOf("", "", "", "")
    }

    @Test
    fun `should only filter no items`() {
        val source = listOf(1, 2, 3, 4, 5).toObservable()
        val testObserver = source.skipFirstUnless { it == 1 }.test()
        val list = testObserver.values()
        list `should be equal to` listOf(1, 2, 3, 4, 5)
    }
}