package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
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
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.btc.BtcCryptoWalletAccount
import piuk.blockchain.android.coincore.btc.BtcOnChainTxEngine
import piuk.blockchain.android.coincore.impl.BitPayInvoiceTarget
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager

@Suppress("SameParameterValue")
class BitpayTxEngineTest : CoincoreTestBase() {

    private val onChainEngine: BtcOnChainTxEngine = mock()
    private val bitPayDataManager: BitPayDataManager = mock()
    private val walletPrefs: WalletStatus = mock()
    private val analytics: Analytics = mock()

    private val subject = BitpayTxEngine(
        bitPayDataManager = bitPayDataManager,
        assetEngine = onChainEngine,
        walletPrefs = walletPrefs,
        analytics = analytics
    )

    @Before
    fun setup() {
        initMocks()
    }

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
        verify(sourceAccount, atLeastOnce()).asset
        verify(onChainEngine).assertInputsValid()
        verifyOnChainEngineStarted(sourceAccount)

        noMoreInteractions(sourceAccount, txTarget)
    }

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
    fun `inputs fail validation when source account incorrect`() {
        val sourceAccount = mock<CustodialTradingAccount>()
        val txTarget = mockTransactionTarget()
        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source asset is not BTC incorrect`() {
        val sourceAccount = mock<CryptoNonCustodialAccount>() {
            on { asset }.thenReturn(WRONG_ASSET)
        }
        val txTarget = mockTransactionTarget()

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
        val invoiceAmount = 100.bitcoin()
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget(invoiceAmount)

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

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == invoiceAmount
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }
            .assertNoErrors()
            .assertComplete()

        verifyOnChainEngineStarted(sourceAccount)
        verify(onChainEngine).doInitialiseTx()
        verify(txTarget).amount

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount has no effect`() {
        // Arrange
        val totalBalance = 21.bitcoin()
        val actionableBalance = 20.bitcoin()
        val invoiceAmount = 100.bitcoin()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockTransactionTarget(invoiceAmount)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = invoiceAmount,
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

        val inputAmount = 2.bitcoin()

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertValue {
                it.amount == invoiceAmount
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }
            .assertComplete()
            .assertNoErrors()

        verifyOnChainEngineStarted(sourceAccount)

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
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS
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
    fun `update fee level from PRIORITY to REGULAR is rejected`() {
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
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Regular,
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
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS
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
    fun `update fee level from PRIORITY to PRIORITY has no effect`() {
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
            FeeLevel.Priority,
            -1
        ).test()
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }
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

    private fun mockTransactionTarget(
        invoiceAmount: CryptoValue = CryptoValue.zero(ASSET)
    ) = mock<BitPayInvoiceTarget> {
        on { asset }.thenReturn(ASSET)
        on { amount }.thenReturn(invoiceAmount)
    }

    private fun verifyOnChainEngineStarted(sourceAccount: CryptoAccount) {
        verify(onChainEngine).start(
            sourceAccount = eq(sourceAccount),
            txTarget = argThat { this is BitPayInvoiceTarget },
            exchangeRates = eq(exchangeRates),
            refreshTrigger = any()
        )
    }

    private fun verifyFeeLevels(feeSelection: FeeSelection, expectedLevel: FeeLevel) =
        feeSelection.selectedLevel == expectedLevel &&
            feeSelection.availableLevels == EXPECTED_AVAILABLE_FEE_LEVELS &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.asset == FEE_ASSET &&
            feeSelection.customAmount == -1L

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(sourceAccount)

        verifyNoMoreInteractions(bitPayDataManager)
        verifyNoMoreInteractions(walletPrefs)
        verifyNoMoreInteractions(analytics)
        verifyNoMoreInteractions(onChainEngine)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
    }

    companion object {
        private val ASSET = CryptoCurrency.BTC
        private val WRONG_ASSET = CryptoCurrency.XLM
        private val FEE_ASSET = CryptoCurrency.BTC

        private val EXPECTED_AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Priority)
    }
}
