package piuk.blockchain.android.coincore.impl.txEngine.interest

import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimits
import com.blockchain.testutils.bitcoin
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.btc.BtcCryptoWalletAccount
import piuk.blockchain.android.coincore.impl.CryptoInterestAccount
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase

class InterestDepositOnChainTxEngineTest : CoincoreTestBase() {

    private val walletManager: CustodialWalletManager = mock()
    private val interestBalances: InterestBalanceDataManager = mock()
    private val onChainEngine: OnChainTxEngineBase = mock()

    private val subject = InterestDepositOnChainTxEngine(
        walletManager = walletManager,
        interestBalances = interestBalances,
        onChainEngine = onChainEngine
    )

    @Before
    fun setup() {
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
    }

    @Ignore("restore once start engine returns completable")
    @Test
    fun `inputs validate when correct`() {
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(txTarget, atLeastOnce()).asset
        verify(sourceAccount, atLeastOnce()).asset
        verify(onChainEngine).assertInputsValid()
        verifyOnChainEngineStarted(sourceAccount)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Ignore("restore once start engine returns completable")
    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when target account incorrect`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when on chain engine validation fails`() {
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        whenever(onChainEngine.assertInputsValid()).thenThrow(IllegalStateException())

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
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
        verifyOnChainEngineStarted(sourceAccount)

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

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(ASSET),
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            feeForFullAvailable = CryptoValue.zero(ASSET),
            feeAmount = CryptoValue.zero(ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = setOf(FeeLevel.Regular, FeeLevel.Priority),
                asset = FEE_ASSET
            )
        )

        whenever(onChainEngine.doInitialiseTx()).thenReturn(Single.just(pendingTx))

        val limits = mock<InterestLimits> {
            on { minDepositFiatValue }.thenReturn(MIN_DEPOSIT_AMOUNT_FIAT)
            on { cryptoCurrency }.thenReturn(ASSET)
        }

        whenever(walletManager.getInterestLimits(ASSET)).thenReturn(Single.just(limits))

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
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
        verifyOnChainEngineStarted(sourceAccount)

        verify(onChainEngine).doInitialiseTx()
        verify(walletManager).getInterestLimits(ASSET)
        verify(exchangeRates).getLastCryptoToFiatRate(ASSET, TEST_API_FIAT)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `when initialising, if getInterestLimits() returns error, then initialisation fails`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(ASSET),
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            feeForFullAvailable = CryptoValue.zero(ASSET),
            feeAmount = CryptoValue.zero(ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = setOf(FeeLevel.Regular, FeeLevel.Priority),
                asset = FEE_ASSET
            )
        )

        whenever(onChainEngine.doInitialiseTx()).thenReturn(Single.just(pendingTx))
        whenever(walletManager.getInterestLimits(ASSET))
            .thenReturn(Single.error(NoSuchElementException()))

        // Act
        subject.doInitialiseTx()
            .test()
            .assertError(NoSuchElementException::class.java)

        verify(sourceAccount, atLeastOnce()).asset
        verifyOnChainEngineStarted(sourceAccount)

        verify(onChainEngine).doInitialiseTx()
        verify(walletManager).getInterestLimits(ASSET)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount delegates to the on-chain engine`() {
        // Arrange
        val totalBalance = 21.bitcoin()
        val actionableBalance = 20.bitcoin()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(ASSET),
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            feeForFullAvailable = CryptoValue.zero(ASSET),
            feeAmount = CryptoValue.zero(ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = FEE_ASSET
            )
        )

        val inputAmount = 2.bitcoin()
        whenever(onChainEngine.doUpdateAmount(inputAmount, pendingTx))
            .thenReturn(
                Single.just(pendingTx.copy(amount = inputAmount))
            )

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertValue {
                it.amount == inputAmount
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }
            .assertComplete()
            .assertNoErrors()

        verifyOnChainEngineStarted(sourceAccount)
        verify(onChainEngine).doUpdateAmount(inputAmount, pendingTx)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from PRIORITY to NONE is rejected`() {
        // Arrange
        val totalBalance = 21.bitcoin()
        val actionableBalance = 20.bitcoin()
        val inputAmount = 2.bitcoin()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            feeForFullAvailable = CryptoValue.zero(ASSET),
            feeAmount = CryptoValue.zero(ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Priority,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = FEE_ASSET
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.None,
            -1
        ).test()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from REGULAR to PRIORITY is rejected`() {
        // Arrange
        val totalBalance = 21.bitcoin()
        val actionableBalance = 20.bitcoin()
        val inputAmount = 2.bitcoin()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            feeForFullAvailable = CryptoValue.zero(ASSET),
            feeAmount = CryptoValue.zero(ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = FEE_ASSET
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Priority,
            -1
        ).test()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from PRIORITY to CUSTOM is rejected`() {
        // Arrange
        val totalBalance = 21.bitcoin()
        val actionableBalance = 20.bitcoin()
        val inputAmount = 2.bitcoin()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            feeForFullAvailable = CryptoValue.zero(ASSET),
            feeAmount = CryptoValue.zero(ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Priority,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = FEE_ASSET
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Custom,
            100
        ).test()
    }

    @Test
    fun `update fee level from REGULAR to REGULAR has no effect`() {
        // Arrange
        val totalBalance = 21.bitcoin()
        val actionableBalance = 20.bitcoin()
        val inputAmount = 2.bitcoin()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            feeForFullAvailable = CryptoValue.zero(ASSET),
            feeAmount = CryptoValue.zero(ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = FEE_ASSET
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Regular,
            -1
        ).test()
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }
            .assertComplete()
            .assertNoErrors()

        verifyOnChainEngineStarted(sourceAccount)

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun mockSourceAccount(
        totalBalance: Money = CryptoValue.zero(ASSET),
        availableBalance: Money = CryptoValue.zero(ASSET)
    ) = mock<BtcCryptoWalletAccount> {
        on { asset }.thenReturn(ASSET)
        on { accountBalance }.thenReturn(Single.just(totalBalance))
        on { actionableBalance }.thenReturn(Single.just(availableBalance))
    }

    private fun mockTransactionTarget() = mock<CryptoInterestAccount> {
        on { asset }.thenReturn(ASSET)
    }

    private fun verifyOnChainEngineStarted(sourceAccount: CryptoAccount) {
        verify(onChainEngine).start(
            sourceAccount = eq(sourceAccount),
            txTarget = argThat { this is CryptoInterestAccount },
            exchangeRates = eq(exchangeRates),
            refreshTrigger = any()
        )
    }

    @Suppress("SameParameterValue")
    private fun verifyFeeLevels(feeSelection: FeeSelection, expectedLevel: FeeLevel) =
        feeSelection.selectedLevel == expectedLevel &&
            feeSelection.availableLevels == EXPECTED_AVAILABLE_FEE_LEVELS &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.asset == FEE_ASSET &&
            feeSelection.customAmount == -1L

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(onChainEngine)
        verifyNoMoreInteractions(walletManager)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(sourceAccount)
    }

    companion object {
        private val ASSET = CryptoCurrency.BTC
        private val WRONG_ASSET = CryptoCurrency.XLM
        private val FEE_ASSET = CryptoCurrency.BTC

        private val ASSET_TO_API_FIAT_RATE = 10.toBigDecimal()
        private val ASSET_TO_USER_FIAT_RATE = 5.toBigDecimal()
        private val MIN_DEPOSIT_AMOUNT_FIAT = FiatValue.fromMajor(TEST_API_FIAT, 10.toBigDecimal())
        private val MIN_DEPOSIT_AMOUNT_CRYPTO = CryptoValue.fromMajor(ASSET, 1.toBigDecimal())
        private val EXPECTED_AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular)
    }
}
