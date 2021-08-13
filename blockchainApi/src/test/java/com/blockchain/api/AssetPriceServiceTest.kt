package com.blockchain.api

import com.blockchain.api.assetprice.AssetPriceApiInterface
import com.blockchain.api.assetprice.data.AssetPriceDto
import com.blockchain.api.assetprice.data.AvailableSymbolsDto
import com.blockchain.api.assetprice.data.PriceRequestPairDto
import com.blockchain.api.assetprice.data.PriceSymbolDto
import com.blockchain.api.services.AssetPriceService
import com.blockchain.api.services.AssetSymbol
import com.blockchain.api.services.PriceTimescale
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test

class AssetPriceServiceTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
        computationTrampoline()
        singleTrampoline()
    }

    private val mockApi: AssetPriceApiInterface = mock()

    private val subject = AssetPriceService(
        api = mockApi,
        apiCode = API_CODE
    )

    @Test
    fun `getSupportedCurrencies returns asset lists`() {

        val symbolBtc = PriceSymbolDto(
            code = TEST_CRYPTO_BTC,
            ticker = TEST_CRYPTO_BTC,
            name = "Bitcoin",
            precisionDp = 8,
            isFiat = false
        )

        val symbolEth = PriceSymbolDto(
            code = TEST_CRYPTO_ETH,
            ticker = TEST_CRYPTO_ETH,
            name = "Ether",
            precisionDp = 18,
            isFiat = false
        )

        val symbolYen = PriceSymbolDto(
            code = TEST_FIAT,
            ticker = TEST_FIAT,
            name = "Yen",
            precisionDp = 0,
            isFiat = true
        )

        val expectedApiDto = AvailableSymbolsDto(
            baseSymbols = mapOf(
                TEST_CRYPTO_BTC to symbolBtc,
                TEST_CRYPTO_ETH to symbolEth
            ),
            quoteSymbols = mapOf(
                TEST_CRYPTO_BTC to symbolBtc,
                TEST_FIAT to symbolYen
            )
        )

        whenever(mockApi.getAvailableSymbols(API_CODE))
            .thenReturn(
                Single.just(expectedApiDto)
            )

        subject.getSupportedCurrencies()
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.base.size == 2 &&
                    it.quote.size == 2 &&
                    validateSymbol(it.base[0], symbolBtc) &&
                    validateSymbol(it.base[1], symbolEth) &&
                    validateSymbol(it.quote[0], symbolBtc) &&
                    validateSymbol(it.quote[1], symbolYen)
            }

        verify(mockApi).getAvailableSymbols(API_CODE)
        verifyNoMoreInteractions(mockApi)
    }

    private fun validateSymbol(result: AssetSymbol, input: PriceSymbolDto) =
        result.isFiat == input.isFiat &&
            result.name == input.name &&
            result.ticker == input.ticker &&
            result.precisionDp == input.precisionDp

    @Test
    fun `getCurrentAssetPrice() correctly returns a price`() {
        val expectedApiInputDto = PriceRequestPairDto(
            crypto = TEST_CRYPTO_BTC,
            fiat = TEST_FIAT
        )

        val timestamp = 1000001L
        val assetPrice = 2000.0
        val expectedApiOutputDto =
            AssetPriceDto(
                timestamp = timestamp,
                price = assetPrice,
                volume24h = null
            )

        val expectedApiPairName = "$TEST_CRYPTO_BTC-$TEST_FIAT"

        whenever(
            mockApi.getCurrentPrices(
                pairs = listOf(expectedApiInputDto),
                apiKey = API_CODE
            )
        ).thenReturn(
            Single.just(
                mapOf(
                    expectedApiPairName to expectedApiOutputDto
                )
            )
        )

        subject.getCurrentAssetPrice(
            TEST_CRYPTO_BTC, TEST_FIAT
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.price == assetPrice &&
                    it.timestamp == timestamp
            }

        verify(mockApi).getCurrentPrices(
            pairs = listOf(expectedApiInputDto),
            apiKey = API_CODE
        )
        verifyNoMoreInteractions(mockApi)
    }

    @Test
    fun `getHistoricPrice() correctly returns a price`() {
        val expectedApiInputDto = PriceRequestPairDto(
            crypto = TEST_CRYPTO_BTC,
            fiat = TEST_FIAT
        )

        val timestamp = 1000001L
        val assetPrice = 2000.0
        val expectedApiOutputDto =
            AssetPriceDto(
                timestamp = timestamp,
                price = assetPrice,
                volume24h = null
            )

        val expectedApiPairName = "$TEST_CRYPTO_BTC-$TEST_FIAT"

        whenever(
            mockApi.getHistoricPrices(
                pairs = listOf(expectedApiInputDto),
                time = timestamp,
                apiKey = API_CODE
            )
        ).thenReturn(
            Single.just(
                mapOf(
                    expectedApiPairName to expectedApiOutputDto
                )
            )
        )

        subject.getHistoricPrice(
            crypto = TEST_CRYPTO_BTC,
            fiat = TEST_FIAT,
            time = timestamp
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.price == assetPrice &&
                    it.timestamp == timestamp
            }

        verify(mockApi).getHistoricPrices(
            pairs = listOf(expectedApiInputDto),
            time = timestamp,
            apiKey = API_CODE
        )
        verifyNoMoreInteractions(mockApi)
    }

    @Test
    fun `getCurrentPrices() correctly returns a map of prices`() {
        // Arrange
        val expectedApiInputBtcDto = PriceRequestPairDto(
            crypto = TEST_CRYPTO_BTC,
            fiat = TEST_FIAT
        )

        val expectedApiInputEthDto = PriceRequestPairDto(
            crypto = TEST_CRYPTO_ETH,
            fiat = TEST_FIAT
        )

        val expectedApiPairNameBtc = "$TEST_CRYPTO_BTC-$TEST_FIAT"
        val expectedApiPairNameEth = "$TEST_CRYPTO_ETH-$TEST_FIAT"

        val timestampBtc = 1000001L
        val assetPriceBtc = 2000.0
        val expectedApiOutputBtcDto =
            AssetPriceDto(
                timestamp = timestampBtc,
                price = assetPriceBtc,
                volume24h = null
            )

        val timestampEth = 1000017L
        val assetPriceEth = 300.0
        val expectedApiOutputEthDto =
            AssetPriceDto(
                timestamp = timestampEth,
                price = assetPriceEth,
                volume24h = null
            )

        whenever(
            mockApi.getCurrentPrices(
                pairs = listOf(expectedApiInputBtcDto, expectedApiInputEthDto),
                apiKey = API_CODE
            )
        ).thenReturn(
            Single.just(
                mapOf(
                    expectedApiPairNameBtc to expectedApiOutputBtcDto,
                    expectedApiPairNameEth to expectedApiOutputEthDto
                )
            )
        )

        subject.getCurrentPrices(
            cryptoTickerList = setOf(TEST_CRYPTO_BTC, TEST_CRYPTO_ETH),
            fiat = TEST_FIAT
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.size == 2 &&
                    it[TEST_CRYPTO_BTC] != null &&
                    it[TEST_CRYPTO_BTC]?.price == assetPriceBtc &&
                    it[TEST_CRYPTO_ETH] != null &&
                    it[TEST_CRYPTO_ETH]?.price == assetPriceEth
            }

        verify(mockApi).getCurrentPrices(
            pairs = listOf(expectedApiInputBtcDto, expectedApiInputEthDto),
            apiKey = API_CODE
        )
        verifyNoMoreInteractions(mockApi)
    }

    @Test
    fun `getHistoricPriceSince() correctly returns a list of prices`() {

        val timestamp1 = 1000001L
        val timestamp2 = 1000001L
        val timestamp3 = 1000001L
        val assetPrice1 = 2000.0
        val assetPrice2 = null
        val assetPrice3 = 2001.0

        val expectedApiOutputDto1 =
            AssetPriceDto(
                timestamp = timestamp1,
                price = assetPrice1,
                volume24h = null
            )
        val expectedApiOutputDto2 =
            AssetPriceDto(
                timestamp = timestamp2,
                price = assetPrice2,
                volume24h = null
            )
        val expectedApiOutputDto3 =
            AssetPriceDto(
                timestamp = timestamp3,
                price = assetPrice3,
                volume24h = null
            )

        whenever(
            mockApi.getHistoricPriceSince(
                crypto = TEST_CRYPTO_BTC,
                fiat = TEST_FIAT,
                start = timestamp1,
                scale = PriceTimescale.ONE_DAY.intervalSeconds,
                apiKey = API_CODE
            )
        ).thenReturn(
            Single.just(
                listOf(
                    expectedApiOutputDto1,
                    expectedApiOutputDto2,
                    expectedApiOutputDto3
                )
            )
        )

        subject.getHistoricPriceSince(
            crypto = TEST_CRYPTO_BTC,
            fiat = TEST_FIAT,
            start = timestamp1,
            scale = PriceTimescale.ONE_DAY
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.size == 2 &&
                    it[0].price == assetPrice1 &&
                    it[1].price == assetPrice3
            }

        verify(mockApi).getHistoricPriceSince(
            crypto = TEST_CRYPTO_BTC,
            fiat = TEST_FIAT,
            start = timestamp1,
            scale = PriceTimescale.ONE_DAY.intervalSeconds,
            apiKey = API_CODE
        )
        verifyNoMoreInteractions(mockApi)
    }

    companion object {
        private const val API_CODE = "BLOCKCHAIN_API_CODE"

        private const val TEST_CRYPTO_BTC = "BTC"
        private const val TEST_CRYPTO_ETH = "ETH"
        private const val TEST_FIAT = "JPY"
    }
}
