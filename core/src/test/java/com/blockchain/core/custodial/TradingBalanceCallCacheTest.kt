package com.blockchain.core.custodial

import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.api.services.TradingBalance
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.nabu.util.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test

class TradingBalanceCallCacheTest {
    companion object {
        private val anyTradingBalance = TradingBalance(
            pending = 2.toBigInteger(),
            total = 10.toBigInteger(),
            actionable = 3.toBigInteger()
        )

        private val expectedBTCBalance = Balance(
            total = CryptoValue.fromMinor(CryptoCurrency.BTC, 10.toBigInteger()),
            actionable = CryptoValue.fromMinor(CryptoCurrency.BTC, 3.toBigInteger()),
            pending = CryptoValue.fromMinor(CryptoCurrency.BTC, 2.toBigInteger())
        )

        private val expectedEURBalance = Balance(
            total = FiatValue.fromMinor("EUR", 10.toLong()),
            actionable = FiatValue.fromMinor("EUR", 3.toLong()),
            pending = FiatValue.fromMinor("EUR", 2.toLong())
        )

        private const val expectedHeader: String = "some_header"
    }

    private val balanceService: CustodialBalanceService = mock()
    private val authHeaderProvider: AuthHeaderProvider = mock()

    private lateinit var subject: TradingBalanceCallCache

    @Before
    fun setup() {
        whenever(authHeaderProvider.getAuthHeader()).thenReturn(Single.just(expectedHeader))
        givenATradingBalanceFor("BTC", "EUR")

        subject = TradingBalanceCallCache(balanceService, authHeaderProvider)
    }

    @Test
    fun `get balance for a given asset`() {
        subject.getBalanceForAsset(CryptoCurrency.BTC)
            .test().waitForCompletionWithoutErrors().assertValue {
                it == expectedBTCBalance
            }
    }

    @Test
    fun `get balance for a given fiat`() {
        subject.getBalanceForFiat("EUR")
            .test().waitForCompletionWithoutErrors().assertValue {
                it == expectedEURBalance
            }
    }

    private fun givenATradingBalanceFor(vararg assetCode: String) {
        val mapEntries = Single.just((assetCode.map { Pair(it, anyTradingBalance) }).toMap())
        whenever(
            balanceService.getTradingBalanceForAllAssets(expectedHeader)
        ).thenReturn(
            mapEntries
        )
    }
}