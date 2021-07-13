package info.blockchain.wallet.prices

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class MockCurrentPriceApi(
    private val base: AssetInfo,
    private val quoteFiat: String,
    private val currentPriceApi: CurrentPriceApi = mock()
) : CurrentPriceApi by currentPriceApi {

    fun givenPrice(price: Double, delaySeconds: Long = 0): MockCurrentPriceApi {
        whenever(currentPriceApi.currentPrice(base, quoteFiat)).thenReturn(if (delaySeconds == 0L) {
            Single.just(BigDecimal.valueOf(price))
        } else {
            Single.timer(delaySeconds, TimeUnit.SECONDS)
                .map { BigDecimal.valueOf(price) }
        })
        return this
    }

    fun givenAnError() {
        whenever(
            currentPriceApi.currentPrice(
                base,
                quoteFiat
            )
        ).thenReturn(Single.error(RuntimeException()))
    }

    fun verifyNumberOfApiCalls(expectedCalls: Int) {
        verify(currentPriceApi, times(expectedCalls))
            .currentPrice(base, quoteFiat)
        verifyNoMoreInteractions(currentPriceApi)
    }
}

fun givenCachedIndicativeFiatPriceService(currentPriceApi: CurrentPriceApi) =
    currentPriceApi.toCachedIndicativeFiatPriceService()

fun <T> Observable<T>.assertSingle(): T =
    test().values().single()
