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
import java.math.RoundingMode

class CachedIndicativeFiatPriceServiceInverseTest {

    private val testScheduler = TestScheduler()

    @get:Rule
    val rx = rxInit {
        computation(testScheduler)
    }

    @Test
    fun `immediate initial request`() {
        val mockPriceApi = MockCurrentPriceApi(
            CryptoCurrency.BTC,
            "GBP"
        ).givenPrice(99.0)

        givenCachedIndicativeFiatPriceService(mockPriceApi)
            .indicativeRateStream(fromFiat = "GBP", to = CryptoCurrency.BTC)
            .assertSingle()
            .apply {
                rate `should be equal to` BigDecimal.ONE.divide(99.toBigDecimal(), 8, RoundingMode.HALF_UP)
                from `should be equal to` "GBP"
                to `should be equal to` CryptoCurrency.BTC
            }
        mockPriceApi.verifyNumberOfApiCalls(1)
    }

    @Test
    fun `opposite requests are cached, so multiple subscriptions do not cause multiple hits to server`() {
        val mockPriceApi = MockCurrentPriceApi(
            CryptoCurrency.BCH,
            "USD"
        ).givenPrice(99.0)

        val service = givenCachedIndicativeFiatPriceService(mockPriceApi)
        service
            .indicativeRateStream(fromFiat = "USD", to = CryptoCurrency.BCH)
            .test()

        service
            .indicativeRateStream(from = CryptoCurrency.BCH, toFiat = "USD")
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
            .indicativeRateStream(from = CryptoCurrency.BCH, toFiat = "USD")
            .test()

        service
            .indicativeRateStream(from = CryptoCurrency.ETHER, toFiat = "GBP")
            .test()

        verify(mockPriceApi).currentPrice(CryptoCurrency.BCH, "GBP")
        verify(mockPriceApi).currentPrice(CryptoCurrency.BCH, "USD")
        verify(mockPriceApi).currentPrice(CryptoCurrency.ETHER, "GBP")
        verifyNoMoreInteractions(mockPriceApi)
    }
}
