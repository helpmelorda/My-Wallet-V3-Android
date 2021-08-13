package piuk.blockchain.android.coincore.impl

import com.blockchain.core.price.ExchangeRate
import com.blockchain.preferences.WalletStatus
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import com.nhaarman.mockitokotlin2.mock

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase

@Suppress("TestFunctionName")
fun STUB_THIS(): Nothing = throw NotImplementedError("This method should be mocked")

class OnChainTxEngineBaseTest : CoincoreTestBase() {

    private val walletPreferences: WalletStatus = mock()
    private val sourceAccount: CryptoAccount = mock()
    private val txTarget: TransactionTarget = mock()

    private class OnChainTxEngineTestSubject(
        requireSecondPassword: Boolean,
        walletPreferences: WalletStatus
    ) : OnChainTxEngineBase(
        requireSecondPassword,
        walletPreferences
    ) {
        override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
            STUB_THIS()
        }
        override fun doInitialiseTx(): Single<PendingTx> {
            STUB_THIS()
        }
        override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
            STUB_THIS()
        }
        override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> {
            STUB_THIS()
        }
        override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
            STUB_THIS()
        }
        override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
            STUB_THIS()
        }
    }

    private val subject: OnChainTxEngineBase =
        OnChainTxEngineTestSubject(
            requireSecondPassword = false,
            walletPreferences = walletPreferences
        )

    @Before
    fun setup() {
        initMocks()
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        whenever(sourceAccount.asset).thenReturn(ASSET)

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        assertEquals(asset, ASSET)

        verify(sourceAccount).asset

        noMoreInteractions()
    }

    @Test
    fun `userFiat returns value from stored prefs`() {
        // Arrange

        // Act
        val result = subject.userFiat

        assertEquals(result, TEST_USER_FIAT)
        verify(currencyPrefs).selectedFiatCurrency

        noMoreInteractions()
    }

    @Test
    fun `exchange rate stream is returned`() {
        // Arrange
        whenever(sourceAccount.asset).thenReturn(ASSET)
        whenever(exchangeRates.getLastCryptoToUserFiatRate(ASSET))
            .thenReturn(
                ExchangeRate.CryptoToFiat(
                    from = ASSET,
                    to = TEST_USER_FIAT,
                    rate = EXCHANGE_RATE
                )
            )

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.userExchangeRate()
            .test()
            .assertValueAt(0) {
                it.rate == EXCHANGE_RATE
            }
            .assertComplete()
            .assertNoErrors()

        verify(sourceAccount, atLeastOnce()).asset
        verify(exchangeRates).getLastCryptoToUserFiatRate(ASSET)

        noMoreInteractions()
    }

    @Test
    fun `confirmations are refreshed`() {
        // Arrange
        val balance = CryptoValue.fromMajor(ASSET, 10.1.toBigDecimal())
        whenever(sourceAccount.accountBalance).thenReturn(Single.just(balance))

        val refreshTrigger = object : TxEngine.RefreshTrigger {
            override fun refreshConfirmations(revalidate: Boolean): Completable =
                Completable.fromAction { sourceAccount.accountBalance }
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates,
            refreshTrigger
        )

        subject.refreshConfirmations(false)

        // Assert
        verify(sourceAccount).accountBalance

        noMoreInteractions()
    }

    private fun noMoreInteractions() {
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private val ASSET = CryptoCurrency.ETHER
        private val EXCHANGE_RATE = 0.01.toBigDecimal()
    }
}
