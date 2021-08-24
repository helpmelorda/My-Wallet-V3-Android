package com.blockchain.core.price.impl

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.AssetPrice
import com.blockchain.api.services.AssetPriceService
import com.blockchain.api.services.PriceTimescale
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import com.nhaarman.mockitokotlin2.any
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class ExchangeRatesDataManagerImplTest {

    private val priceService: AssetPriceService = mock {
        on {
            getHistoricPriceSince(
                crypto = any(),
                fiat = any(),
                start = any(),
                scale = any()
            )
        }.thenReturn(Single.just(PRICE_DATA))
    }

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency }.thenReturn(SELECTED_FIAT)
    }

    private val calendar = Calendar.getInstance().apply {
        timeInMillis = DATE_NOW_MILLIS
    }

    private val priceStore: AssetPriceStore = mock()
    private val sparklineCall: SparklineCallCache = mock()

    private val subject = ExchangeRatesDataManagerImpl(
        priceStore = priceStore,
        sparklineCall = sparklineCall,
        assetPriceService = priceService,
        currencyPrefs = currencyPrefs
    )

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Test
    fun `get All Time Price`() {
        subject.getHistoricPriceSeries(OLD_ASSET, HistoricalTimeSpan.ALL_TIME)
            .test()
            .assertComplete()
            .assertNoErrors()

        verify(priceService).getHistoricPriceSince(
            crypto = OLD_ASSET.ticker,
            fiat = SELECTED_FIAT,
            start = OLD_ASSET.startDate!!,
            scale = PriceTimescale.FIVE_DAYS
        )
        verifyNoMoreInteractions(priceService)
    }

    @Test
    fun getYearPrice() {
        subject.getHistoricPriceSeries(
            OLD_ASSET,
            HistoricalTimeSpan.YEAR,
            calendar
        ).test()
            .assertComplete()
            .assertNoErrors()

        verify(priceService).getHistoricPriceSince(
            OLD_ASSET.ticker,
            SELECTED_FIAT,
            DATE_ONE_YEAR_AGO_SECS,
            PriceTimescale.ONE_DAY
        )
        verifyNoMoreInteractions(priceService)
    }

    @Test
    fun getMonthPrice() {

        subject.getHistoricPriceSeries(
            OLD_ASSET,
            HistoricalTimeSpan.MONTH,
            calendar
        ).test()
            .assertComplete()
            .assertNoErrors()

        verify(priceService).getHistoricPriceSince(
            OLD_ASSET.ticker,
            SELECTED_FIAT,
            DATE_ONE_MONTH_AGO_SECS,
            PriceTimescale.TWO_HOURS
        )
        verifyNoMoreInteractions(priceService)
    }

    @Test
    fun getWeekPrice() {
        subject.getHistoricPriceSeries(
            OLD_ASSET,
            HistoricalTimeSpan.WEEK,
            calendar
        ).test()
            .assertComplete()
            .assertNoErrors()

        verify(priceService).getHistoricPriceSince(
            OLD_ASSET.ticker,
            SELECTED_FIAT,
            DATE_ONE_WEEK_AGO_SECS,
            PriceTimescale.ONE_HOUR
        )
        verifyNoMoreInteractions(priceService)
    }

    @Test
    fun getDayPrice() {
        subject.getHistoricPriceSeries(
            OLD_ASSET,
            HistoricalTimeSpan.DAY,
        calendar
        ).test()
            .assertComplete()
            .assertNoErrors()

        verify(priceService).getHistoricPriceSince(
            OLD_ASSET.ticker,
            SELECTED_FIAT,
            DATE_ONE_DAY_AGO_SECS,
            PriceTimescale.FIFTEEN_MINUTES
        )
        verifyNoMoreInteractions(priceService)
    }

    @Test
    fun `get year price on new asset`() {
        subject.getHistoricPriceSeries(
            NEW_ASSET,
            HistoricalTimeSpan.WEEK,
            calendar
        ).test()
            .assertComplete()
            .assertNoErrors()

        verify(priceService).getHistoricPriceSince(
            OLD_ASSET.ticker,
            SELECTED_FIAT,
            NEW_ASSET.startDate!!,
            PriceTimescale.ONE_HOUR
        )
        verifyNoMoreInteractions(priceService)
    }
    companion object {
        private const val DATE_NOW_MILLIS = 1626972500000L
        private const val DATE_ONE_YEAR_AGO_SECS = 1595436500L
        private const val DATE_ONE_MONTH_AGO_SECS = 1624380500L
        private const val DATE_ONE_WEEK_AGO_SECS = 1626367700L
        private const val DATE_ONE_DAY_AGO_SECS = 1626886100L

        private const val SELECTED_FIAT = "USD"

        private val OLD_ASSET = object : CryptoCurrency(
            ticker = "DUMMY",
            name = "Dummies",
            startDate = 1000000001,
            categories = emptySet(),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        ) { }

        private val NEW_ASSET = object : CryptoCurrency(
            ticker = "DUMMY",
            name = "Dummies",
            startDate = DATE_ONE_WEEK_AGO_SECS,
            categories = emptySet(),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        ) { }

        private val PRICE_DATA = listOf(
            AssetPrice(
                100.toDouble(), 200000
            )
        )
    }
}