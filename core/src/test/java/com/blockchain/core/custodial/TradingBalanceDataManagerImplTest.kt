package com.blockchain.core.custodial

import com.blockchain.android.testutils.rxInit
import com.blockchain.testutils.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test

class TradingBalanceDataManagerImplTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    private val tradingBalanceCallCache: TradingBalanceCallCache = mock()
    private val subject = TradingBalanceDataManagerImpl(
        balanceCallCache = tradingBalanceCallCache
    )

    @Test
    fun `will get balance for a given asset`() {
        val cacheResult = buildCacheResult(
            crypto = listOf(CRYPTO_ASSET_1, CRYPTO_ASSET_2),
            fiat = listOf(FIAT_TICKER_1, FIAT_TICKER_2)
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getBalanceForAsset(CRYPTO_ASSET_1)
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                val total = it.total
                total is CryptoValue &&
                total.currency == CRYPTO_ASSET_1 &&
                total.isPositive
            }
    }

    @Test
    fun `balance not found for a given asset`() {
        val cacheResult = buildCacheResult(
            crypto = listOf(CRYPTO_ASSET_1),
            fiat = emptyList()
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getBalanceForAsset(CRYPTO_ASSET_2)
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                val total = it.total
                total is CryptoValue &&
                total.currency == CRYPTO_ASSET_2 &&
                total.isZero
            }
    }

    @Test
    fun `will get balance for a given fiat`() {
        val cacheResult = buildCacheResult(
            crypto = listOf(CRYPTO_ASSET_1, CRYPTO_ASSET_2),
            fiat = listOf(FIAT_TICKER_1, FIAT_TICKER_2)
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getBalanceForFiat(FIAT_TICKER_1)
            .test()
            .await()
            .assertValue {
                val total = it.total as? FiatValue
                total is FiatValue &&
                    total.currencyCode == FIAT_TICKER_1 &&
                    total.isPositive
            }
    }

    @Test
    fun `balance not found for a given fiat`() {
        val cacheResult = buildCacheResult(
            crypto = listOf(CRYPTO_ASSET_1),
            fiat = listOf(FIAT_TICKER_2)
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getBalanceForFiat(FIAT_TICKER_1)
            .test()
            .await()
            .assertValue {
                val total = it.total
                total is FiatValue &&
                    total.currencyCode == FIAT_TICKER_1 &&
                    total.isZero
            }
    }

    @Test
    fun `get active assets`() {
        val cacheResult = buildCacheResult(
            crypto = listOf(CRYPTO_ASSET_1, CRYPTO_ASSET_2),
            fiat = listOf(FIAT_TICKER_2)
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getActiveAssets()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.size == 2 &&
                    it.contains(CRYPTO_ASSET_1) &&
                    it.contains(CRYPTO_ASSET_2)
            }
    }

    @Test
    fun `there are no active assets`() {
        val cacheResult = buildCacheResult(
            crypto = emptyList(),
            fiat = listOf(FIAT_TICKER_2)
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getActiveAssets()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.isEmpty()
            }
    }

    @Test
    fun `get active fiat`() {
        val cacheResult = buildCacheResult(
            crypto = listOf(CRYPTO_ASSET_1, CRYPTO_ASSET_2),
            fiat = listOf(FIAT_TICKER_2)
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getActiveFiats()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.size == 1 && it.contains(FIAT_TICKER_2)
            }
    }

    @Test
    fun `there are no active fiats`() {
        val cacheResult = buildCacheResult(
            crypto = emptyList(),
            fiat = emptyList()
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getActiveFiats()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.isEmpty()
            }
    }

    private fun anyBalanceForAsset(asset: AssetInfo): TradingAccountBalance =
        TradingAccountBalance(
            total = CryptoValue.fromMinor(asset, 1.toBigInteger()),
            actionable = CryptoValue.fromMinor(asset, 2.toBigInteger()),
            pending = CryptoValue.fromMinor(asset, 3.toBigInteger())
        )

    private fun anyBalanceForFiat(fiat: String): TradingAccountBalance =
        TradingAccountBalance(
            total = FiatValue.fromMinor(fiat, 1.toLong()),
            actionable = FiatValue.fromMinor(fiat, 2.toLong()),
            pending = FiatValue.fromMinor(fiat, 3.toLong())
        )

    private fun buildCacheResult(crypto: List<AssetInfo>, fiat: List<String>): TradingBalanceRecord =
        TradingBalanceRecord(
            cryptoBalances = crypto.map { it to anyBalanceForAsset(it) }.toMap(),
            fiatBalances = fiat.map { it to anyBalanceForFiat(it) }.toMap()
        )

    companion object {
        private const val CRYPTO_TICKER_1 = "CRYPTO1"
        private const val CRYPTO_TICKER_2 = "CRYPTO2"
        private const val FIAT_TICKER_1 = "USD"
        private const val FIAT_TICKER_2 = "GBP"

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
    }
}
