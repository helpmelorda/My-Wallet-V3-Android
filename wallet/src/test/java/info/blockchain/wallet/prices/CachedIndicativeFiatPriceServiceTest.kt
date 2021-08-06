package info.blockchain.wallet.prices

import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.amshove.kluent.`should be equal to`
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class CachedIndicativeFiatPriceServiceTest {

    private val testScheduler = TestScheduler()

    @get:Rule
    val rx = rxInit {
        computation(testScheduler)
    }

    @Test
    fun `immediate initial request`() {
        val mockPriceApi = MockCurrentPriceApi(CryptoCurrency.BTC, "USD")
            .givenPrice(99.0)

        givenCachedIndicativeFiatPriceService(mockPriceApi)
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .assertSingle()
            .apply {
                rate `should be equal to` BigDecimal.valueOf(99.0)
                from `should be equal to` CryptoCurrency.BTC
                to `should be equal to` "USD"
            }
        mockPriceApi.verifyNumberOfApiCalls(1)
    }

    @Test
    fun `two values`() {
        val mockPriceApi = MockCurrentPriceApi(CryptoCurrency.BTC, "USD")
            .givenPrice(99.0)

        val test = givenCachedIndicativeFiatPriceService(mockPriceApi)
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .test()

        testScheduler.advanceTimeBy(999, TimeUnit.MILLISECONDS)
        mockPriceApi.givenPrice(100.0)
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        test.values().map { it.rate } `should be equal to` listOf(BigDecimal.valueOf(99.0), BigDecimal.valueOf(100.0))
        mockPriceApi.verifyNumberOfApiCalls(2)
    }

    @Test
    fun `given a long period the calls are repeated`() {
        val mockPriceApi = MockCurrentPriceApi(CryptoCurrency.BTC, "USD")
            .givenPrice(99.0)

        givenCachedIndicativeFiatPriceService(mockPriceApi)
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .test()

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        mockPriceApi.verifyNumberOfApiCalls(6)
    }

    @Test
    fun `given a delay in getting the price, the number of requests is not increased`() {
        val mockPriceApi = MockCurrentPriceApi(CryptoCurrency.BTC, "USD")
            .givenPrice(99.0, delaySeconds = 5)

        val test = givenCachedIndicativeFiatPriceService(mockPriceApi)
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .test()

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        mockPriceApi.givenPrice(100.0)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        mockPriceApi.verifyNumberOfApiCalls(2)
        test.values().map { it.rate } `should be equal to` listOf(BigDecimal.valueOf(99.0), BigDecimal.valueOf(100.0))
    }

    @Test
    fun `given an error, the request recovers`() {
        val mockPriceApi = MockCurrentPriceApi(CryptoCurrency.BTC, "USD")
            .givenPrice(99.0)

        val test = mockPriceApi.toIndicativeFiatPriceService()
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .test()

        mockPriceApi.givenAnError()
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        mockPriceApi.givenPrice(100.0)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        test.assertNoErrors()
        test.values().map { it.rate } `should be equal to` listOf(BigDecimal.valueOf(99.0), BigDecimal.valueOf(100.0))
        mockPriceApi.verifyNumberOfApiCalls(3)
    }

    @Test
    fun `given a persistent error, the request recovers`() {
        val mockPriceApi = MockCurrentPriceApi(CryptoCurrency.BTC, "USD")
            .givenPrice(99.0)

        val test = mockPriceApi.toIndicativeFiatPriceService()
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .test()

        mockPriceApi.givenAnError()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        mockPriceApi.givenPrice(100.0)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        test.assertNoErrors()
        test.values().map { it.rate } `should be equal to` listOf(BigDecimal.valueOf(99.0), BigDecimal.valueOf(100.0))
        mockPriceApi.verifyNumberOfApiCalls(11)
    }

    @Test
    fun `requests are cached, so multiple subscriptions do not cause multiple hits to server`() {
        val mockPriceApi = MockCurrentPriceApi(CryptoCurrency.BTC, "USD").givenPrice(99.0)

        val service = givenCachedIndicativeFiatPriceService(mockPriceApi)
        service
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .test()

        service
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .test()

        mockPriceApi.verifyNumberOfApiCalls(1)
    }

    @Test
    fun `different pairs result in multiple calls`() {
        val mockPriceApi = mock<CurrentPriceApi>()

        val service = givenCachedIndicativeFiatPriceService(mockPriceApi)
        service
            .indicativeRateStream(fromFiat = "GBP", to = CryptoCurrency.BCH)
            .test()

        service
            .indicativeRateStream(fromFiat = "USD", to = CryptoCurrency.BCH)
            .test()

        service
            .indicativeRateStream(fromFiat = "USD", to = CryptoCurrency.ETHER)
            .test()

        verify(mockPriceApi).currentPrice(CryptoCurrency.BCH, "GBP")
        verify(mockPriceApi).currentPrice(CryptoCurrency.BCH, "USD")
        verify(mockPriceApi).currentPrice(CryptoCurrency.ETHER, "USD")
        verifyNoMoreInteractions(mockPriceApi)
    }
}
