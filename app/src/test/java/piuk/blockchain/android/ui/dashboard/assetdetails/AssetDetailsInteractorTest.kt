package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import junit.framework.Assert.assertEquals
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAsset
import java.util.Locale

class AssetDetailsInteractorTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private val totalGroup: AccountGroup = mock()
    private val nonCustodialGroup: AccountGroup = mock()
    private val custodialGroup: AccountGroup = mock()
    private val interestGroup: AccountGroup = mock()
    private val interestRate: Double = 5.0
    private val interestEnabled: Boolean = true
    private val asset: CryptoAsset = mock {
        on { accountGroup(AssetFilter.All) }.thenReturn(Maybe.just(totalGroup))
        on { accountGroup(AssetFilter.NonCustodial) }.thenReturn(Maybe.just(nonCustodialGroup))
        on { accountGroup(AssetFilter.Custodial) }.thenReturn(Maybe.just(custodialGroup))
        on { accountGroup(AssetFilter.Interest) }.thenReturn(Maybe.just(interestGroup))
    }
    private val featureFlagMock: FeatureFlag = mock {
        on { enabled }.thenReturn(Single.just(interestEnabled))
    }

    private val subject = AssetDetailsInteractor(featureFlagMock, mock(), mock(), mock())

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `cryptoBalance,fiatBalance & interestBalance return the right values`() {

        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 56478.99.toBigDecimal())

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.zero(CryptoCurrency.BTC)
        val interestCrypto = CryptoValue.zero(CryptoCurrency.BTC)
        val pendingCrypto = CryptoValue.zero(CryptoCurrency.BTC)

        val walletFiat = FiatValue.fromMinor("USD", 30985)
        val custodialFiat = FiatValue.fromMinor("USD", 0)
        val interestFiat = FiatValue.fromMinor("USD", 0)

        val expectedResult = mapOf(
            AssetFilter.NonCustodial to AssetDisplayInfo(nonCustodialGroup,
                walletCrypto,
                pendingCrypto,
                walletFiat,
                emptySet()),
            AssetFilter.Custodial to AssetDisplayInfo(
                custodialGroup,
                custodialCrypto,
                pendingCrypto,
                custodialFiat,
                emptySet()),
            AssetFilter.Interest to AssetDisplayInfo(
                interestGroup, interestCrypto, pendingCrypto, interestFiat, emptySet(), interestRate
            )
        )

        whenever(asset.exchangeRate()).thenReturn(Single.just(price))

        whenever(nonCustodialGroup.accountBalance).thenReturn(Single.just(walletCrypto))
        whenever(nonCustodialGroup.pendingBalance).thenReturn(Single.just(pendingCrypto))
        whenever(nonCustodialGroup.isEnabled).thenReturn(Single.just(true))
        whenever(nonCustodialGroup.actions).thenReturn(Single.just(emptySet()))
        whenever(custodialGroup.accountBalance).thenReturn(Single.just(custodialCrypto))
        whenever(custodialGroup.pendingBalance).thenReturn(Single.just(pendingCrypto))
        whenever(custodialGroup.actions).thenReturn(Single.just(emptySet()))
        whenever(custodialGroup.isEnabled).thenReturn(Single.just(true))
        whenever(interestGroup.accountBalance).thenReturn(Single.just(interestCrypto))
        whenever(interestGroup.pendingBalance).thenReturn(Single.just(pendingCrypto))
        whenever(interestGroup.isEnabled).thenReturn(Single.just(true))
        whenever(interestGroup.actions).thenReturn(Single.just(emptySet()))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        whenever(custodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(custodialGroup.isFunded).thenReturn(true)
        whenever(nonCustodialGroup.isFunded).thenReturn(true)

        whenever(interestGroup.accounts).thenReturn(listOf(mock()))
        whenever(interestGroup.isFunded).thenReturn(true)

        val v = subject.loadAssetDetails(asset)
            .test()
            .values()

        // Using assertResult(expectedResult) instead of fetching the values and checking them results in
        // an 'AssertionException Not completed' result. I have no clue why; changing the matchers to not use all
        // three possible enum values changes the failure into an expected 'Failed, not equal' result (hence the
        // doAnswer() nonsense instead of eq() etc - I tried many things)
        // All very strange.
        assertEquals(expectedResult, v[0])
    }

    @Test
    fun `cryptoBalance, fiatBalance & interestBalance are never returned if exchange rate fails`() {
        whenever(asset.exchangeRate()).thenReturn(Single.error(Throwable()))

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.zero(CryptoCurrency.BTC)
        val interestCrypto = CryptoValue.zero(CryptoCurrency.BTC)

        whenever(nonCustodialGroup.accountBalance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.accountBalance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.accountBalance).thenReturn(Single.just(interestCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        val testObserver = subject.loadAssetDetails(asset)
            .test()

        testObserver.assertNoValues()
    }

    @Test
    fun `cryptoBalance & fiatBalance never return if interest fails`() {
        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.zero(CryptoCurrency.BTC)

        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 5647899.toBigDecimal())

        whenever(asset.exchangeRate()).thenReturn(Single.just(price))
        whenever(asset.accountGroup(AssetFilter.Interest)).thenReturn(Maybe.error(Throwable()))

        whenever(nonCustodialGroup.accountBalance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.accountBalance).thenReturn(Single.just(custodialCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        val testObserver = subject.loadAssetDetails(asset)
            .test()
        testObserver.assertNoValues()
    }

    @Test
    fun `exchange rate is the right one`() {
        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 56478.99.toBigDecimal())

        whenever(asset.exchangeRate()).thenReturn(Single.just(price))

        val testObserver = subject.loadExchangeRate(asset).test()
        testObserver.assertValue("$56,478.99").assertValueCount(1)
    }

    @Test
    fun `historic prices are returned properly`() {
        val ratesList = listOf(
            HistoricalRate(5556, 2.toDouble()),
            HistoricalRate(587, 22.toDouble()),
            HistoricalRate(6981, 23.toDouble())
        )

        whenever(asset.historicRateSeries(HistoricalTimeSpan.DAY))
            .thenReturn(Single.just(ratesList))

        subject.loadHistoricPrices(asset, HistoricalTimeSpan.DAY)
            .test()
            .assertValue { it == ratesList }
            .assertValueCount(1)
            .assertNoErrors()
    }

    @Test
    fun `when historic prices api returns error, empty list should be returned`() {
        whenever(asset.historicRateSeries(HistoricalTimeSpan.DAY))
            .thenReturn(Single.error(Throwable()))

        subject.loadHistoricPrices(asset, HistoricalTimeSpan.DAY)
            .test()
            .assertValue { it.isEmpty() }
            .assertValueCount(1)
            .assertNoErrors()
    }
}