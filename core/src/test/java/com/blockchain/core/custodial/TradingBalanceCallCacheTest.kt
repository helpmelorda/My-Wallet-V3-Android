package com.blockchain.core.custodial

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.api.services.TradingBalance
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.testutils.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test

class TradingBalanceCallCacheTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    private val authHeaderProvider: AuthHeaderProvider = mock {
        on { getAuthHeader() }.thenReturn(Single.just(EXPECTED_HEADER))
    }

    private val assetCatalogue: AssetCatalogue = mock {
        on { fromNetworkTicker(CRYPTO_TICKER_1) }.thenReturn(CRYPTO_ASSET_1)
        on { fromNetworkTicker(CRYPTO_TICKER_2) }.thenReturn(CRYPTO_ASSET_2)
        on { fromNetworkTicker(FIAT_TICKER_1) }.thenReturn(null)
        on { fromNetworkTicker(FIAT_TICKER_2) }.thenReturn(null)
        on { fromNetworkTicker(UNKNOWN_TICKER) }.thenReturn(null)

        on { isFiatTicker(CRYPTO_TICKER_1) }.thenReturn(false)
        on { isFiatTicker(CRYPTO_TICKER_2) }.thenReturn(false)
        on { isFiatTicker(FIAT_TICKER_1) }.thenReturn(true)
        on { isFiatTicker(FIAT_TICKER_2) }.thenReturn(true)
        on { isFiatTicker(UNKNOWN_TICKER) }.thenReturn(false)
    }

    private val balanceService: CustodialBalanceService = mock()

    private val subject = TradingBalanceCallCache(
        balanceService,
        assetCatalogue,
        authHeaderProvider
    )

    @Test
    fun `cache is transformed correctly`() {
        givenATradingBalanceFor(
            CRYPTO_TICKER_1,
            CRYPTO_TICKER_2,
            FIAT_TICKER_1,
            FIAT_TICKER_2,
            UNKNOWN_TICKER
        )

        subject.getTradingBalances()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.cryptoBalances.keys.size == 2 &&
                    it.fiatBalances.keys.size == 2
            }.assertValue {
                it.cryptoBalances.keys.containsAll(setOf(CRYPTO_ASSET_1, CRYPTO_ASSET_2))
            }.assertValue {
                it.fiatBalances.keys.containsAll(setOf(FIAT_TICKER_1, FIAT_TICKER_2))
            }
    }

    private fun givenATradingBalanceFor(vararg assetCode: String) {
        val mapEntries = assetCode.map { makeTradingBalanceFor(it) }
        whenever(
            balanceService.getTradingBalanceForAllAssets(EXPECTED_HEADER)
        ).thenReturn(
            Single.just(mapEntries)
        )
    }

    private fun makeTradingBalanceFor(symbol: String) =
        TradingBalance(
            assetTicker = symbol,
            pending = 2.toBigInteger(),
            total = 10.toBigInteger(),
            actionable = 3.toBigInteger()
        )

    companion object {
        private const val CRYPTO_TICKER_1 = "CRYPTO1"
        private const val CRYPTO_TICKER_2 = "CRYPTO2"
        private const val FIAT_TICKER_1 = "USD"
        private const val FIAT_TICKER_2 = "GBP"
        private const val UNKNOWN_TICKER = "NOPE!"

        private val CRYPTO_ASSET_1 = object : CryptoCurrency(
            ticker = CRYPTO_TICKER_1,
            name = "Crypto_1",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        ) { }

        private val CRYPTO_ASSET_2 = object : CryptoCurrency(
            ticker = CRYPTO_TICKER_2,
            name = "Crypto_2",
            categories = setOf(AssetCategory.CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        ) { }

        private const val EXPECTED_HEADER = "some_header"
    }
}