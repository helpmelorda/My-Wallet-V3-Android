package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AccountBalance
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

        val price = ExchangeRate.CryptoToFiat(TEST_ASSET, TEST_FIAT, 56478.99.toBigDecimal())

        val walletBalance = AccountBalance(
            total = CryptoValue(TEST_ASSET, 548621.toBigInteger()),
            actionable = CryptoValue(TEST_ASSET, 548621.toBigInteger()),
            pending = CryptoValue.zero(TEST_ASSET),
            exchangeRate = price
        )
        val custodialBalance = AccountBalance(
            total = CryptoValue.zero(TEST_ASSET),
            actionable = CryptoValue.zero(TEST_ASSET),
            pending = CryptoValue.zero(TEST_ASSET),
            exchangeRate = price
        )
        val interestBalance = AccountBalance(
            total = CryptoValue.zero(TEST_ASSET),
            actionable = CryptoValue.zero(TEST_ASSET),
            pending = CryptoValue.zero(TEST_ASSET),
            exchangeRate = price
        )

        val walletFiat = FiatValue.fromMinor(TEST_FIAT, 30985)
        val custodialFiat = FiatValue.fromMinor(TEST_FIAT, 0)
        val interestFiat = FiatValue.fromMinor(TEST_FIAT, 0)

        val expectedResult = mapOf(
            AssetFilter.NonCustodial to AssetDisplayInfo(
                nonCustodialGroup,
                walletBalance.total,
                walletBalance.pending,
                walletFiat,
                emptySet()
            ),
            AssetFilter.Custodial to AssetDisplayInfo(
                custodialGroup,
                custodialBalance.total,
                custodialBalance.pending,
                custodialFiat,
                emptySet()
            ),
            AssetFilter.Interest to AssetDisplayInfo(
                interestGroup,
                interestBalance.total,
                interestBalance.pending,
                interestFiat,
                emptySet(),
                interestRate
            )
        )

        whenever(asset.exchangeRate()).thenReturn(Single.just(price))

        whenever(nonCustodialGroup.balance).thenReturn(Observable.just(walletBalance))
        whenever(nonCustodialGroup.isEnabled).thenReturn(Single.just(true))
        whenever(nonCustodialGroup.actions).thenReturn(Single.just(emptySet()))

        whenever(custodialGroup.balance).thenReturn(Observable.just(custodialBalance))
        whenever(custodialGroup.isEnabled).thenReturn(Single.just(true))
        whenever(custodialGroup.actions).thenReturn(Single.just(emptySet()))

        whenever(interestGroup.balance).thenReturn(Observable.just(interestBalance))
        whenever(interestGroup.isEnabled).thenReturn(Single.just(true))
        whenever(interestGroup.actions).thenReturn(Single.just(emptySet()))

        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        whenever(custodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(custodialGroup.isFunded).thenReturn(true)
        whenever(nonCustodialGroup.isFunded).thenReturn(true)

        whenever(interestGroup.accounts).thenReturn(listOf(mock()))
        whenever(interestGroup.isFunded).thenReturn(true)

        subject.loadAssetDetails(asset)
            .test()
            .assertValueCount(1)
            .assertValueAt(0) {
                it == expectedResult
            }
    }

    @Test
    fun `cryptoBalance, fiatBalance & interestBalance are never returned if exchange rate fails`() {
        whenever(asset.exchangeRate()).thenReturn(Single.error(Throwable()))

        val walletCrypto = CryptoValue(TEST_ASSET, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.zero(TEST_ASSET)
        val interestCrypto = CryptoValue.zero(TEST_ASSET)

        whenever(nonCustodialGroup.accountBalance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.accountBalance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.accountBalance).thenReturn(Single.just(interestCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        subject.loadAssetDetails(asset)
            .test()
            .assertNoValues()
    }

    @Test
    fun `cryptoBalance & fiatBalance never return if interest fails`() {
        val walletCrypto = CryptoValue(TEST_ASSET, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.zero(TEST_ASSET)

        val price = ExchangeRate.CryptoToFiat(TEST_ASSET, TEST_FIAT, 5647899.toBigDecimal())

        whenever(asset.exchangeRate()).thenReturn(Single.just(price))
        whenever(asset.accountGroup(AssetFilter.Interest)).thenReturn(Maybe.error(Throwable()))

        whenever(nonCustodialGroup.accountBalance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.accountBalance).thenReturn(Single.just(custodialCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        subject.loadAssetDetails(asset)
            .test()
            .assertNoValues()
    }

    @Test
    fun `exchange rate is the right one`() {
        val price = ExchangeRate.CryptoToFiat(TEST_ASSET, TEST_FIAT, 56478.99.toBigDecimal())

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

    companion object {
        private const val TEST_FIAT = "USD"

        private val TEST_ASSET = object : CryptoCurrency(
            ticker = "NOPE",
            name = "Not a real thing",
            categories = setOf(AssetCategory.NON_CUSTODIAL, AssetCategory.CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 3,
            colour = "000000"
        ) {}
    }
}