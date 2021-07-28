package info.blockchain.wallet.api

import com.blockchain.nabu.util.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.PriceEndpoints
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.rxjava3.core.Single
import org.junit.Test
import java.util.Calendar

class PriceApiTest {
    companion object {
        private const val assetCode: String = "eth"
        private const val fiat: String = "gbp"
    }

    private val priceEndpoints: PriceEndpoints = mock()
    private val apiCode: ApiCode = mock()
    private val subject: PriceApi = PriceApi(priceEndpoints, apiCode)

    @Test
    fun `get historic price series`() {
        val oneYearAgo = oneYearAgoInSeconds()
        val scale = TimeInterval.ONE_DAY.intervalSeconds

        val expectedResult = arrayListOf(
            PriceDatum(),
            PriceDatum(timestamp = 1, price = 0.5, volume24h = 10.5)
        )

        whenever(
            priceEndpoints.getHistoricPriceSeries(assetCode, fiat, oneYearAgo, scale, apiCode.apiCode)
        ).thenReturn(
            Single.just(expectedResult)
        )

        subject.getHistoricPriceSeries(
            assetCode,
            fiat,
            oneYearAgo,
            scale
        ).test().waitForCompletionWithoutErrors().assertValue {
            it == expectedResult
        }
    }

    @Test
    fun `get current price`() {
        val expectedPrice: Double = 123.45

        whenever(
            priceEndpoints.getCurrentPrice(assetCode, fiat, apiCode.apiCode)
        ).thenReturn(
            Single.just(PriceDatum(price = expectedPrice))
        )

        subject.getCurrentPrice(assetCode, fiat).test()
            .waitForCompletionWithoutErrors().assertValue {
                it == expectedPrice
            }
    }

    @Test
    fun `get historic price`() {
        val epoch = 1L
        val expectedPrice = 2.34

        whenever(
            priceEndpoints.getHistoricPrice(assetCode, fiat, epoch, apiCode.apiCode)
        ).thenReturn(
            Single.just(PriceDatum(price = expectedPrice))
        )

        subject.getHistoricPrice(assetCode, fiat, epoch).test()
            .waitForCompletionWithoutErrors().assertValue {
                it == expectedPrice
            }
    }

    @Test
    fun `get price indexes`() {
        val expectedResult: Map<String, PriceDatum> = mapOf(assetCode to PriceDatum())

        whenever(
            priceEndpoints.getPriceIndexes(assetCode, apiCode.apiCode)
        ).thenReturn(
            Single.just(expectedResult)
        )

        subject.getPriceIndexes(assetCode).test()
            .waitForCompletionWithoutErrors().assertValue {
                it == expectedResult
            }
    }

    private fun oneYearAgoInSeconds(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -365)
        return cal.timeInMillis / 1000
    }
}