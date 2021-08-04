package com.blockchain.core.price.impl

import com.blockchain.api.services.AssetPriceService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import org.junit.Rule
import java.util.Calendar

class ExchangeRatesDataManagerTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
        singleTrampoline()
    }

    private val priceStore: AssetPriceStore = mock()
    private val sparklineCall: SparklineCallCache = mock()
    private val assetPriceService: AssetPriceService = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val calendar: Calendar = mock()

    private val subject = ExchangeRatesDataManagerImpl(
        priceStore = priceStore,
        sparklineCall = sparklineCall,
        assetPriceService = assetPriceService,
        currencyPrefs = currencyPrefs,
        calendar = calendar
    )

//    @Test
//    fun getHistoricPrice() {
//        givenHistoricExchangeRate(CryptoCurrency.BTC, "USD", 100L, 8100.37.toBigDecimal())
//        subject.getHistoricPrice(1.bitcoin(), "USD", 100L)
//            .test()
//            .values()
//            .first()
//            .apply {
//                this `should be equal to` 8100.37.usd()
//            }
//    }
//
// @Test
// fun `BTC toFiat`() {
//    givenExchangeRate(CryptoCurrency.BTC, "USD", 5000.0)
//
//    0.01.bitcoin().toFiat(subject, "USD") `should be equal to` 50.usd()
// }
//
//    @Test
//    fun `BCH toFiat`() {
//        givenExchangeRate(CryptoCurrency.BCH, "USD", 1000.0)
//
//        0.1.bitcoinCash().toFiat(subject, "USD") `should be equal to` 100.usd()
//    }
//
//    @Test
//    fun `ETH toFiat`() {
//        givenExchangeRate(CryptoCurrency.ETHER, "USD", 1000.0)
//
//        2.ether().toFiat(subject, "USD") `should be equal to` 2000.usd()
//    }
//
//    @Test
//    fun `USD toCrypto BTC`() {
//        givenExchangeRate(CryptoCurrency.BTC, "USD", 5000.0)
//
//        50.usd().toCrypto(subject, CryptoCurrency.BTC) `should be equal to` 0.01.bitcoin()
//    }
//
//    @Test
//    fun `USD toCrypto BCH`() {
//        givenExchangeRate(CryptoCurrency.BCH, "USD", 1000.0)
//
//        100.usd().toCrypto(subject, CryptoCurrency.BCH) `should be equal to` 0.1.bitcoinCash()
//    }
//
//    @Test
//    fun `USD toCrypto ETHER`() {
//        givenExchangeRate(CryptoCurrency.ETHER, "USD", 1000.0)
//
//        2000.usd().toCrypto(subject, CryptoCurrency.ETHER) `should be equal to` 2.ether()
//    }
//
//    @Test
//    fun `toCrypto when no rate, but zero anyway`() {
//        0.usd().toCrypto(subject, CryptoCurrency.ETHER) `should be equal to` 0.ether()
//        0.usd().toCryptoOrNull(subject, CryptoCurrency.ETHER) `should be equal to` 0.ether()
//    }
//
//    @Test
//    fun `toCrypto when no rate, but not zero`() {
//        1.usd().toCrypto(subject, CryptoCurrency.BCH) `should be equal to` 0.bitcoinCash()
//        1.usd().toCryptoOrNull(subject, CryptoCurrency.BCH) `should be equal to` null
//    }
//
//    @Test
//    fun `toCrypto yields full precision of the currency - BTC`() {
//        givenExchangeRate(CryptoCurrency.BTC, "USD", 5610.82)
//        1000.82.usd().toCrypto(subject, CryptoCurrency.BTC) `should be equal to` 0.17837321.bitcoin()
//    }
//
//    @Test
//    fun `toCrypto yields full precision of the currency - ETH`() {
//        givenExchangeRate(CryptoCurrency.ETHER, "USD", 5610.83)
//        val expected = BigDecimal("0.178372896701557526").ether()
//
//        val result = BigDecimal(1000.82).usd().toCrypto(subject, CryptoCurrency.ETHER)
//        result `should be equal to` expected
//    }
//
//    @Test
//    fun `toCrypto yields full precision of the currency - XLM`() {
//        givenExchangeRate(CryptoCurrency.XLM, "USD", 5610.82)
//        1000.82.usd().toCrypto(subject, CryptoCurrency.XLM) `should be equal to` 0.1783732.lumens()
//    }
//
//    @Test
//    fun `toCrypto rounds up on half`() {
//        givenExchangeRate(CryptoCurrency.BTC, "USD", 9.0)
//        5.usd().toCrypto(subject, CryptoCurrency.BTC) `should be equal to` 0.55555556.bitcoin()
//    }
//
//    private fun givenExchangeRate(
//        cryptoCurrency: AssetInfo,
//        currencyName: String,
//        exchangeRate: Double
//    ) {
//        whenever(exchangeRateDataStore.getLastPrice(cryptoCurrency, currencyName)).thenReturn(exchangeRate)
//    }
//
//    private fun givenHistoricExchangeRate(
//        cryptoCurrency: AssetInfo,
//        currencyName: String,
//        time: Long,
//        price: BigDecimal
//    ) {
//        whenever(exchangeRateDataStore.getHistoricPrice(cryptoCurrency, currencyName, time))
//            .thenReturn(Single.just(price))
//    }
}
