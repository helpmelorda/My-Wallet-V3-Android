package com.blockchain.core.custodial

import com.blockchain.nabu.util.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Maybe
import org.junit.Test

class TradingBalanceDataManagerImplTest {
    private val tradingBalanceCallCache: TradingBalanceCallCache = mock()
    private val subject = TradingBalanceDataManagerImpl(tradingBalanceCallCache = tradingBalanceCallCache)

    @Test
    fun `will get total balance for a given asset`() {
        val asset = CryptoCurrency.BTC
        val expectedBalance = anyBalanceForAsset(asset)

        whenever(
            tradingBalanceCallCache.getBalanceForAsset(asset)
        ).thenReturn(
            Maybe.just(expectedBalance)
        )

        subject.getTotalBalanceForAsset(asset)
            .test().waitForCompletionWithoutErrors().assertValue {
                it == expectedBalance.total
            }
    }

    @Test
    fun `will get actionable balance for a given asset`() {
        val asset = CryptoCurrency.BTC
        val expectedBalance = anyBalanceForAsset(asset)

        whenever(
            tradingBalanceCallCache.getBalanceForAsset(asset)
        ).thenReturn(
            Maybe.just(expectedBalance)
        )

        subject.getActionableBalanceForAsset(asset)
            .test().waitForCompletionWithoutErrors().assertValue {
                it == expectedBalance.actionable
            }
    }

    @Test
    fun `will get pending balance for a given asset`() {
        val asset = CryptoCurrency.BTC
        val expectedBalance = anyBalanceForAsset(asset)

        whenever(
            tradingBalanceCallCache.getBalanceForAsset(asset)
        ).thenReturn(
            Maybe.just(expectedBalance)
        )

        subject.getPendingBalanceForAsset(asset)
            .test().waitForCompletionWithoutErrors().assertValue {
                it == expectedBalance.pending
            }
    }

    @Test
    fun `will get total balance for a given fiat`() {
        val fiat = "EUR"
        val expectedBalance = anyBalanceForFiat(fiat)

        whenever(
            tradingBalanceCallCache.getBalanceForFiat(fiat)
        ).thenReturn(
            Maybe.just(expectedBalance)
        )

        subject.getFiatTotalBalanceForAsset(fiat)
            .test().waitForCompletionWithoutErrors().assertValue {
                it == expectedBalance.total
            }
    }

    @Test
    fun `will get pending balance for a given fiat`() {
        val fiat = "EUR"
        val expectedBalance = anyBalanceForFiat(fiat)

        whenever(
            tradingBalanceCallCache.getBalanceForFiat(fiat)
        ).thenReturn(
            Maybe.just(expectedBalance)
        )

        subject.getFiatPendingBalanceForAsset(fiat)
            .test().waitForCompletionWithoutErrors().assertValue {
                it == expectedBalance.pending
            }
    }

    @Test
    fun `will get actionable balance for a given fiat`() {
        val fiat = "EUR"
        val expectedBalance = anyBalanceForFiat(fiat)

        whenever(
            tradingBalanceCallCache.getBalanceForFiat(fiat)
        ).thenReturn(
            Maybe.just(expectedBalance)
        )

        subject.getFiatActionableBalanceForAsset(fiat)
            .test().waitForCompletionWithoutErrors().assertValue {
                it == expectedBalance.actionable
            }
    }

    private fun anyBalanceForAsset(asset: AssetInfo): Balance =
        Balance(
            total = CryptoValue.fromMinor(asset, 1.toBigInteger()),
            actionable = CryptoValue.fromMinor(asset, 2.toBigInteger()),
            pending = CryptoValue.fromMinor(asset, 3.toBigInteger())
        )

    private fun anyBalanceForFiat(fiat: String): Balance =
        Balance(
            total = FiatValue.fromMinor(fiat, 1.toLong()),
            actionable = FiatValue.fromMinor(fiat, 2.toLong()),
            pending = FiatValue.fromMinor(fiat, 3.toLong())
        )
}