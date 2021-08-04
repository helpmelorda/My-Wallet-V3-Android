package piuk.blockchain.android.coincore.impl.txEngine.interest

import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimits
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.btc.BtcCryptoWalletAccount
import piuk.blockchain.android.coincore.impl.CryptoInterestAccount
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase

class InterestDepositTradingEngineTest : CoincoreTestBase() {

    private fun mockTransactionTarget() = mock<CryptoInterestAccount> {
        on { asset }.thenReturn(ASSET)
    }

    private val custodialWalletManager: CustodialWalletManager = mock()

    private lateinit var subject: InterestDepositTradingEngine

    @Before
    fun setUp() {
        initMocks()

        whenever(exchangeRates.getLastCryptoToUserFiatRate(ASSET))
            .thenReturn(
                ExchangeRate.CryptoToFiat(
                    from = ASSET,
                    to = TEST_USER_FIAT,
                    rate = ASSET_TO_USER_FIAT_RATE
                )
            )

        whenever(exchangeRates.getLastCryptoToFiatRate(ASSET, TEST_API_FIAT))
            .thenReturn(
                ExchangeRate.CryptoToFiat(
                    from = ASSET,
                    to = TEST_API_FIAT,
                    rate = ASSET_TO_API_FIAT_RATE
                )
            )

        subject = InterestDepositTradingEngine(custodialWalletManager)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when assets mismatched`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoInterestAccount = mock {
            on { asset }.thenReturn(WRONG_ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        asset shouldEqual ASSET

        verify(sourceAccount, atLeastOnce()).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val limits = mock<InterestLimits> {
            on { minDepositFiatValue }.thenReturn(MIN_DEPOSIT_AMOUNT_FIAT)
            on { cryptoCurrency }.thenReturn(ASSET)
        }

        whenever(custodialWalletManager.getInterestLimits(ASSET)).thenReturn(Maybe.just(limits))

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(ASSET) &&
                    it.totalBalance == CryptoValue.zero(ASSET) &&
                    it.availableBalance == CryptoValue.zero(ASSET) &&
                    it.feeAmount == CryptoValue.zero(ASSET) &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.confirmations.isEmpty() &&
                    it.minLimit == MIN_DEPOSIT_AMOUNT_CRYPTO &&
                    it.maxLimit == null &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset

        verify(custodialWalletManager).getInterestLimits(ASSET)
        verify(currencyPrefs).selectedFiatCurrency
        verify(sourceAccount).accountBalance
        verify(exchangeRates).getLastCryptoToFiatRate(ASSET, TEST_API_FIAT)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `when initialising, if getInterestLimits() returns empty, then initialisation fails`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        whenever(custodialWalletManager.getInterestLimits(ASSET)).thenReturn(Maybe.empty())

        // Act
        subject.doInitialiseTx()
            .test()
            .assertError(NoSuchElementException::class.java)

        verify(sourceAccount, atLeastOnce()).asset

        verify(custodialWalletManager).getInterestLimits(ASSET)
        verify(sourceAccount).accountBalance

        noMoreInteractions(sourceAccount, txTarget)

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(custodialWalletManager)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(sourceAccount)
    }

    private fun mockSourceAccount(
        totalBalance: Money = CryptoValue.zero(ASSET),
        availableBalance: Money = CryptoValue.zero(ASSET)
    ) = mock<BtcCryptoWalletAccount> {
        on { asset }.thenReturn(ASSET)
        on { accountBalance }.thenReturn(Single.just(totalBalance))
        on { actionableBalance }.thenReturn(Single.just(availableBalance))
    }

    companion object {
        private val ASSET = CryptoCurrency.BTC
        private val WRONG_ASSET = CryptoCurrency.XLM

        private val ASSET_TO_API_FIAT_RATE = 10.toBigDecimal()
        private val ASSET_TO_USER_FIAT_RATE = 5.toBigDecimal()
        private val MIN_DEPOSIT_AMOUNT_FIAT = FiatValue.fromMajor(TEST_API_FIAT, 10.toBigDecimal())
        private val MIN_DEPOSIT_AMOUNT_CRYPTO = CryptoValue.fromMajor(ASSET, 1.toBigDecimal())
    }
}