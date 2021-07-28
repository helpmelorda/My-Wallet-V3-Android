package piuk.blockchain.androidcore.data.exchangerate

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.rxjava3.core.Single
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should have key`
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExchangeRateServiceTest {

    private lateinit var subject: ExchangeRateService

    private val historicPriceApi: PriceApi = mock()

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = ExchangeRateService(historicPriceApi)
    }

    @Test
    fun `getAllTimePrice BTC`() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                btc.ticker,
                fiat,
                btc.startDate!!,
                TimeInterval.FIVE_DAYS.intervalSeconds
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(btc, fiat, TimeSpan.ALL_TIME).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            btc.ticker,
            fiat,
            btc.startDate!!,
            TimeInterval.FIVE_DAYS.intervalSeconds
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun `getAllTimePrice ETH`() {
        // Arrange
        val eth = CryptoCurrency.ETHER
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eth.ticker,
                fiat,
                eth.startDate!!,
                TimeInterval.FIVE_DAYS.intervalSeconds
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(eth, fiat, TimeSpan.ALL_TIME).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eth.ticker,
            fiat,
            eth.startDate!!,
            TimeInterval.FIVE_DAYS.intervalSeconds
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getYearPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.ticker),
                eq(fiat),
                any(),
                eq(TimeInterval.ONE_DAY.intervalSeconds)
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(btc, fiat, TimeSpan.YEAR).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.ticker),
            eq(fiat),
            any(),
            eq(TimeInterval.ONE_DAY.intervalSeconds)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getMonthPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.ticker),
                eq(fiat),
                any(),
                eq(TimeInterval.TWO_HOURS.intervalSeconds)
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(btc, fiat, TimeSpan.MONTH).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.ticker),
            eq(fiat),
            any(),
            eq(TimeInterval.TWO_HOURS.intervalSeconds)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getWeekPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.ticker),
                eq(fiat),
                any(),
                eq(TimeInterval.ONE_HOUR.intervalSeconds)
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(btc, fiat, TimeSpan.WEEK).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.ticker),
            eq(fiat),
            any(),
            eq(TimeInterval.ONE_HOUR.intervalSeconds)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getDayPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.ticker),
                eq(fiat),
                any(),
                eq(TimeInterval.FIFTEEN_MINUTES.intervalSeconds)
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(btc, fiat, TimeSpan.DAY).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.ticker),
            eq(fiat),
            any(),
            eq(TimeInterval.FIFTEEN_MINUTES.intervalSeconds)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getExchangeRateMap() {
        val mockApi = mock<PriceApi> {
            on { getPriceIndexes("BTC") }.thenReturn(Single.just(mapOf("USD" to mock())))
        }
        mockApi.getPriceIndexes("BTC").test()
            .values()
            .first()
            .apply {
                this `should have key` "USD"
            }
    }

    @Test
    fun getHistoricPrice() {
        val mockApi = mock<PriceApi> {
            on { getHistoricPrice("ETH", "GBP", 100L) }.thenReturn(Single.just(500.0))
        }
        mockApi.getHistoricPrice("ETH", "GBP", 100).test()
            .values()
            .first()
            .apply {
                this `should be equal to` 500.0
            }
    }
}