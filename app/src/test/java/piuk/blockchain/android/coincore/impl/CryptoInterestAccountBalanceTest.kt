package piuk.blockchain.android.coincore.impl

import com.blockchain.core.interest.InterestAccountBalance
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.testutils.testValue
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.TestScheduler
import junit.framework.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase
import java.util.concurrent.TimeUnit

class CryptoInterestAccountBalanceTest : CoincoreTestBase() {

    private val custodialManager: CustodialWalletManager = mock()
    private val interestBalance: InterestBalanceDataManager = mock()
    private val features: InternalFeatureFlagApi = mock()

    private val subject = CryptoInterestAccount(
        asset = TEST_ASSET,
        label = "Test Account",
        exchangeRates = exchangeRates,
        custodialWalletManager = custodialManager,
        interestBalance = interestBalance,
        features = features
    )

    @Before
    fun setup() {
        initMocks()
    }

    @Test
    fun `Balance is fetched correctly and is non-zero`() {

        whenever(exchangeRates.cryptoToUserFiatRate(TEST_ASSET))
            .thenReturn(Observable.just(TEST_TO_USER_RATE_1))

        val balance = InterestAccountBalance(
            totalBalance = 100.testValue(TEST_ASSET),
            pendingInterest = 90.testValue(TEST_ASSET),
            pendingDeposit = 80.testValue(TEST_ASSET),
            totalInterest = 70.testValue(TEST_ASSET),
            lockedBalance = 60.testValue(TEST_ASSET)
        )

        whenever(interestBalance.getBalanceForAsset(TEST_ASSET))
            .thenReturn(Observable.just(balance))

        subject.balance
            .test()
            .assertComplete()
            .assertValue {
                it.total == balance.totalBalance &&
                it.actionable == 40.testValue(TEST_ASSET) &&
                it.pending == balance.pendingDeposit &&
                it.exchangeRate == TEST_TO_USER_RATE_1
            }

        assert(subject.isFunded)
    }

    @Test
    fun `Balance is fetched correctly and is zero`() {

        whenever(exchangeRates.cryptoToUserFiatRate(TEST_ASSET))
            .thenReturn(Observable.just(TEST_TO_USER_RATE_1))

        val balance = InterestAccountBalance(
            totalBalance = 0.testValue(TEST_ASSET),
            pendingInterest = 0.testValue(TEST_ASSET),
            pendingDeposit = 80.testValue(TEST_ASSET),
            totalInterest = 0.testValue(TEST_ASSET),
            lockedBalance = 0.testValue(TEST_ASSET)
        )

        whenever(interestBalance.getBalanceForAsset(TEST_ASSET))
            .thenReturn(Observable.just(balance))

        subject.balance
            .test()
            .assertComplete()
            .assertValue {
                it.total == balance.totalBalance &&
                it.exchangeRate == TEST_TO_USER_RATE_1
            }

        assertFalse(subject.isFunded)
    }

    @Test
    fun `rate changes are propagated correctly`() {

        val scheduler = TestScheduler()

        val rates = listOf(TEST_TO_USER_RATE_1, TEST_TO_USER_RATE_2)
        val rateSource = Observable.zip(
            Observable.interval(1, TimeUnit.SECONDS, scheduler),
            Observable.fromIterable(rates)
        ) { /* tick */ _, rate -> rate as ExchangeRate }

        whenever(exchangeRates.cryptoToUserFiatRate(TEST_ASSET))
            .thenReturn(rateSource)

        val balance = InterestAccountBalance(
            totalBalance = 100.testValue(TEST_ASSET),
            pendingInterest = 90.testValue(TEST_ASSET),
            pendingDeposit = 80.testValue(TEST_ASSET),
            totalInterest = 70.testValue(TEST_ASSET),
            lockedBalance = 60.testValue(TEST_ASSET)
        )

        whenever(interestBalance.getBalanceForAsset(TEST_ASSET))
            .thenReturn(Observable.just(balance))

        val testSubscriber = subject.balance
            .subscribeOn(scheduler)
            .test()
            .assertNoValues()
            .assertNotComplete()

        scheduler.advanceTimeBy(3, TimeUnit.SECONDS)

        testSubscriber.apply {
            assertValueCount(2)
            assertValueAt(0) { it.total == balance.totalBalance && it.exchangeRate == TEST_TO_USER_RATE_1 }
            assertValueAt(1) { it.total == balance.totalBalance && it.exchangeRate == TEST_TO_USER_RATE_2 }
            assertComplete()
        }
    }

    companion object {

        private val TEST_ASSET = object : CryptoCurrency(
            ticker = "NOPE",
            name = "Not a real thing",
            categories = setOf(AssetCategory.CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 3,
            colour = "000000"
        ) {}

        private val RATE_1 = 1.2.toBigDecimal()
        private val RATE_2 = 1.4.toBigDecimal()

        private val TEST_TO_USER_RATE_1 = ExchangeRate.CryptoToFiat(
            from = TEST_ASSET,
            to = TEST_USER_FIAT,
            rate = RATE_1
        )

        private val TEST_TO_USER_RATE_2 = ExchangeRate.CryptoToFiat(
            from = TEST_ASSET,
            to = TEST_USER_FIAT,
            rate = RATE_2
        )
    }
}
