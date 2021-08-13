package piuk.blockchain.android.coincore.impl.txEngine.interest

import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimits
import com.blockchain.nabu.models.data.CryptoWithdrawalFeeAndLimit
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
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.btc.BtcCryptoWalletAccount
import piuk.blockchain.android.coincore.impl.CryptoInterestAccount
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase
import java.math.BigInteger

class InterestWithdrawOnChainTxEngineTest : CoincoreTestBase() {

    private fun mockTransactionTarget() = mock<BtcCryptoWalletAccount> {
        on { asset }.thenReturn(ASSET)
    }

    private val custodialWalletManager: CustodialWalletManager = mock()

    private lateinit var subject: InterestWithdrawOnChainTxEngine

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

        subject = InterestWithdrawOnChainTxEngine(custodialWalletManager)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when assets mismatched`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: BtcCryptoWalletAccount = mock {
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
            on { maxWithdrawalFiatValue }.thenReturn(MAX_WITHDRAW_AMOUNT_FIAT)
        }

        val fees = mock<CryptoWithdrawalFeeAndLimit> {
            on { minLimit }.thenReturn(MIN_WITHDRAW_AMOUNT)
            on { fee }.thenReturn(BigInteger.ZERO)
        }

        whenever(custodialWalletManager.getInterestLimits(ASSET)).thenReturn(Maybe.just(limits))
        whenever(custodialWalletManager.fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)).thenReturn(
            Single.just(fees)
        )

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
                    it.minLimit == CryptoValue.fromMinor(ASSET, fees.minLimit) &&
                    it.maxLimit == MAX_WITHDRAW_AMOUNT_CRYPTO &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset

        verify(custodialWalletManager).getInterestLimits(ASSET)
        verify(custodialWalletManager).fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)
        verify(currencyPrefs).selectedFiatCurrency
        verify(sourceAccount).actionableBalance
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
        whenever(custodialWalletManager.fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)).thenReturn(
            Single.just(mock())
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertError(NoSuchElementException::class.java)

        verify(sourceAccount, atLeastOnce()).asset

        verify(custodialWalletManager).getInterestLimits(ASSET)
        verify(custodialWalletManager).fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)
        verify(sourceAccount).actionableBalance

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `when initialising, if fetchCryptoWithdrawFeeAndMinLimit() returns error, then initialisation fails`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        whenever(custodialWalletManager.getInterestLimits(ASSET)).thenReturn(Maybe.just(mock()))
        whenever(custodialWalletManager.fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)).thenReturn(
            Single.error(Exception())
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertError(Exception::class.java)

        verify(sourceAccount, atLeastOnce()).asset

        verify(custodialWalletManager).getInterestLimits(ASSET)
        verify(custodialWalletManager).fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)
        verify(sourceAccount).actionableBalance

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `when building confirmations, it add the right ones`() {
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val money = CryptoValue.fromMajor(ASSET, 10.toBigDecimal())
        val mockPendingTx =
            PendingTx(money, money, money, money, money, FeeSelection(), "USD", listOf(), money, money)

        // Act
        subject.doBuildConfirmations(mockPendingTx)
            .test()
            .assertValue { pTx ->
                pTx.confirmations.find { it is TxConfirmationValue.From } != null &&
                    pTx.confirmations.find { it is TxConfirmationValue.To } != null &&
                    pTx.confirmations.find { it is TxConfirmationValue.Total } != null
                pTx.confirmations.find { it is TxConfirmationValue.NetworkFee } != null
            }
            .assertNoErrors()
            .assertComplete()
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
    ) = mock<CryptoInterestAccount> {
        on { asset }.thenReturn(ASSET)
        on { accountBalance }.thenReturn(Single.just(totalBalance))
        on { actionableBalance }.thenReturn(Single.just(availableBalance))
    }

    companion object {
        private val ASSET = CryptoCurrency.BTC
        private val WRONG_ASSET = CryptoCurrency.XLM

        private val ASSET_TO_API_FIAT_RATE = 10.toBigDecimal()
        private val ASSET_TO_USER_FIAT_RATE = 5.toBigDecimal()

        private val MIN_WITHDRAW_AMOUNT = 1.toBigInteger()
        private val MAX_WITHDRAW_AMOUNT_FIAT = FiatValue.fromMajor(TEST_API_FIAT, 10.toBigDecimal())
        private val MAX_WITHDRAW_AMOUNT_CRYPTO = CryptoValue.fromMajor(ASSET, 1.toBigDecimal())
    }
}