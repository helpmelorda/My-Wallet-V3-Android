package piuk.blockchain.android.coincore.impl

import com.blockchain.core.price.ExchangeRate
import com.blockchain.testutils.numberToBigDecimal
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before

import org.junit.Test
import piuk.blockchain.android.coincore.AccountBalance
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase

class CryptoAccountCompoundGroupTest : CoincoreTestBase() {

    @Before
    fun setup() {
        whenever(exchangeRates.cryptoToUserFiatRate(TEST_ASSET))
            .thenReturn(Observable.just(TEST_TO_USER_RATE))
    }

    @Test
    fun `group with single account returns single account balance`() {
        // Arrange
        val accountBalance = AccountBalance(
            total = 100.testValue(),
            pending = 0.testValue(),
            actionable = 100.testValue(),
            exchangeRate = TEST_TO_USER_RATE
        )

        val account: CryptoAccount = mock {
            on { balance }.thenReturn(Observable.just(accountBalance))
        }

        val subject = CryptoAccountNonCustodialGroup(
            asset = TEST_ASSET,
            label = "group label",
            accounts = listOf(account)
        )

        // Act
        subject.balance.test()
            .assertComplete()
            .assertValue {
                it.total == accountBalance.total &&
                    it.actionable == accountBalance.actionable &&
                    it.pending == accountBalance.pending &&
                    it.exchangeRate == TEST_TO_USER_RATE
            }
    }

    @Test
    fun `group with two accounts returns the sum of the account balance`() {
        // Arrange
        val accountBalance1 = AccountBalance(
            total = 100.testValue(),
            pending = 0.testValue(),
            actionable = 100.testValue(),
            exchangeRate = TEST_TO_USER_RATE
        )

        val account1: CryptoAccount = mock {
            on { balance }.thenReturn(Observable.just(accountBalance1))
        }

        val accountBalance2 = AccountBalance(
            total = 50.testValue(),
            pending = 0.testValue(),
            actionable = 40.testValue(),
            exchangeRate = TEST_TO_USER_RATE
        )
        val account2: CryptoAccount = mock {
            on { balance }.thenReturn(Observable.just(accountBalance2))
        }

        val subject = CryptoAccountNonCustodialGroup(
            asset = TEST_ASSET,
            label = "group label",
            accounts = listOf(account1, account2)
        )

        // Act
        subject.balance.test()
            .assertComplete()
            .assertValue {
                it.total == accountBalance1.total + accountBalance2.total &&
                    it.pending == accountBalance1.pending + accountBalance2.pending &&
                    it.actionable == accountBalance1.actionable + accountBalance2.actionable &&
                    it.exchangeRate == TEST_TO_USER_RATE
            }
    }

    @Test
    fun `group with single account returns single account actions`() {
        // Arrange
        val accountActions = setOf(AssetAction.Send, AssetAction.Receive)

        val account: CryptoAccount = mock {
            on { actions }.thenReturn(Single.just(accountActions))
        }

        val subject = CryptoAccountNonCustodialGroup(
            asset = TEST_ASSET,
            label = "group label",
            accounts = listOf(account)
        )

        // Act
        val r = subject.actions.test()

        // Assert
        r.assertValue(setOf(AssetAction.Send, AssetAction.Receive))
    }

    @Test
    fun `group with three accounts returns the union of possible actions`() {
        // Arrange
        val accountActions1 = Single.just(setOf(
            AssetAction.Send,
            AssetAction.Receive
        ))

        val accountActions2 = Single.just(setOf(
            AssetAction.Send,
            AssetAction.Swap
        ))

        val accountActions3 = Single.just(setOf(
            AssetAction.Send,
            AssetAction.Receive
        ))

        val expectedResult = setOf(
            AssetAction.Send,
            AssetAction.Swap,
            AssetAction.Receive
        )

        val account1: CryptoAccount = mock {
            on { actions }.thenReturn(accountActions1)
        }

        val account2: CryptoAccount = mock {
            on { actions }.thenReturn(accountActions2)
        }

        val account3: CryptoAccount = mock {
            on { actions }.thenReturn(accountActions3)
        }

        val subject = CryptoAccountNonCustodialGroup(
            asset = TEST_ASSET,
            label = "group label",
            accounts = listOf(account1, account2, account3)
        )

        // Act
        val r = subject.actions.test()

        // Assert
        r.assertValue(expectedResult)
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

        private val TEST_TO_USER_RATE = ExchangeRate.CryptoToFiat(
            from = TEST_ASSET,
            to = TEST_USER_FIAT,
            rate = 1.2.toBigDecimal()
        )

        fun Number.testValue() = CryptoValue.fromMajor(TEST_ASSET, numberToBigDecimal())
    }
}