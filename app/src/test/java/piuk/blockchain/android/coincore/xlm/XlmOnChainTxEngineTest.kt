package piuk.blockchain.android.coincore.xlm

import com.blockchain.fees.FeeType
import com.blockchain.preferences.WalletStatus
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import com.blockchain.testutils.lumens
import com.blockchain.testutils.stroops
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.coincore.BlockchainAccount
import kotlin.test.assertEquals
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import timber.log.Timber

class XlmOnChainTxEngineTest : CoincoreTestBase() {

    private val xlmDataManager: XlmDataManager = mock()
    private val walletOptionsDataManager: WalletOptionsDataManager = mock {
        on { isXlmAddressExchange(TARGET_ADDRESS) }.thenReturn(false)
        on { isXlmAddressExchange(TARGET_EXCHANGE_ADDRESS) }.thenReturn(true)
    }
    private val xlmFeesFetcher: XlmFeesFetcher = mock {
        on { operationFee(FeeType.Regular) }.thenReturn(Single.just(FEE_REGULAR))
    }

    private val walletPreferences: WalletStatus = mock()

    private val subject = XlmOnChainTxEngine(
        xlmDataManager = xlmDataManager,
        xlmFeesFetcher = xlmFeesFetcher,
        walletOptionsDataManager = walletOptionsDataManager,
        requireSecondPassword = false,
        walletPreferences = walletPreferences
    )

    @Before
    fun setup() {
        initMocks()
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: CryptoAccount = mock {
            on { asset }.thenReturn(ASSET)
        }

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

        // Assert
        verify(txTarget).asset
        verify(sourceAccount).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Asset incorrect`() {
        val sourceAccount: CryptoAccount = mock {
            on { asset }.thenReturn(WRONG_ASSET)
        }

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

        // Assert
        verify(txTarget).asset
        verify(sourceAccount).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val sourceAccount: CryptoAccount = mock {
            on { asset }.thenReturn(ASSET)
        }

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

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

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised for non-exchange address`() {
        // Arrange
        val sourceAccount: CryptoAccount = mock {
            on { asset }.thenReturn(ASSET)
        }

        val txTarget: XlmAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
            on { memo }.thenReturn(MEMO_TEXT)
        }

        whenever(sourceAccount.asset).thenReturn(ASSET)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
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
                it.minLimit == null &&
                it.maxLimit == null &&
                it.validationState == ValidationState.UNINITIALISED &&
                it.engineState.size == 1 &&
                it.engineState[STATE_MEMO]?.let { memo ->
                    memo is TxConfirmationValue.Memo &&
                    memo.text == MEMO_TEXT &&
                    !memo.isRequired &&
                    memo.id == null &&
                    memo.editable
                } ?: false
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget).address
        verify(txTarget).memo
        verify(currencyPrefs).selectedFiatCurrency
        verify(walletOptionsDataManager).isXlmAddressExchange(TARGET_ADDRESS)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised for exchange address`() {
        // Arrange
        val sourceAccount: CryptoAccount = mock {
            on { asset }.thenReturn(ASSET)
        }

        val txTarget: XlmAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_EXCHANGE_ADDRESS)
            on { memo }.thenReturn(MEMO_TEXT)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                Timber.d("$it")
                it.amount == CryptoValue.zero(ASSET) &&
                    it.totalBalance == CryptoValue.zero(ASSET) &&
                    it.availableBalance == CryptoValue.zero(ASSET) &&
                    it.feeAmount == CryptoValue.zero(ASSET) &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.confirmations.isEmpty() &&
                    it.minLimit == null &&
                    it.maxLimit == null &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.size == 1 &&
                    it.engineState[STATE_MEMO]?.let { memo ->
                        memo is TxConfirmationValue.Memo &&
                            memo.text == MEMO_TEXT &&
                            memo.isRequired &&
                            memo.id == null &&
                            memo.editable
                    } ?: false
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget).address
        verify(txTarget).memo
        verify(currencyPrefs).selectedFiatCurrency
        verify(walletOptionsDataManager).isXlmAddressExchange(TARGET_EXCHANGE_ADDRESS)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        // Arrange
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()

        val sourceAccount = fundedSourceAccount(totalBalance, actionableBalance)

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
                asset = CryptoCurrency.XLM
            )
        )

        val inputAmount = 2.lumens()
        val expectedFee = FEE_REGULAR
        val expectedAvailable = actionableBalance - expectedFee
        val expectedFullFee = FEE_REGULAR

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                it.totalBalance == totalBalance &&
                it.availableBalance == expectedAvailable &&
                it.feeForFullAvailable == expectedFullFee &&
                it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        verify(sourceAccount).accountBalance
        verify(sourceAccount).actionableBalance
        verify(xlmFeesFetcher).operationFee(FeeType.Regular)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level to PRIORITY is rejected`() {
        // Arrange
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        val totalBalance = 21.lumens()
        val availableBalance = 20.lumens()
        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val inputAmount = 2.lumens()
        val expectedFee = FEE_REGULAR

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = expectedFee,
            feeAmount = expectedFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS
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
    fun `update fee level to NONE is rejected`() {
        // Arrange
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        val totalBalance = 21.lumens()
        val availableBalance = 20.lumens()
        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val inputAmount = 2.lumens()
        val expectedFee = FEE_REGULAR

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = expectedFee,
            feeAmount = expectedFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
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
    fun `update fee level to CUSTOM is rejected`() {
        // Arrange
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        val totalBalance = 21.lumens()
        val availableBalance = 20.lumens()
        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val inputAmount = 2.lumens()
        val expectedFee = FEE_REGULAR

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = expectedFee,
            feeAmount = expectedFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Custom,
            -1
        ).test()
    }

    @Test
    fun `update fee level to REGULAR has no effect`() {
        // Arrange
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        val totalBalance = 21.lumens()
        val availableBalance = 20.lumens()
        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val inputAmount = 2.lumens()
        val expectedFee = FEE_REGULAR
        val expectedAvailable = availableBalance - expectedFee

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = expectedAvailable,
            feeForFullAvailable = expectedFee,
            feeAmount = expectedFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.XLM
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Regular,
            -1
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                val v = 100
                it.amount == inputAmount &&
                it.totalBalance == totalBalance &&
                it.availableBalance == expectedAvailable &&
                it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun fundedSourceAccount(totalBalance: Money, availableBalance: Money) =
        mock<XlmCryptoWalletAccount> {
            on { asset }.thenReturn(ASSET)
            on { accountBalance }.thenReturn(Single.just(totalBalance))
            on { actionableBalance }.thenReturn(Single.just(availableBalance))
        }

    private fun verifyFeeLevels(
        feeSelection: FeeSelection,
        expectedLevel: FeeLevel
    ) = feeSelection.selectedLevel == expectedLevel &&
        feeSelection.availableLevels == EXPECTED_AVAILABLE_FEE_LEVELS &&
        feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
        feeSelection.asset == FEE_ASSET &&
        feeSelection.customAmount == -1L

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(xlmDataManager)
        verifyNoMoreInteractions(xlmFeesFetcher)
        verifyNoMoreInteractions(walletOptionsDataManager)
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private val ASSET = CryptoCurrency.XLM
        private val WRONG_ASSET = CryptoCurrency.BTC
        private val FEE_ASSET = CryptoCurrency.XLM
        private const val TARGET_ADDRESS = "VALID_NON_EXCHANGE_XLM_ADDRESS"
        private const val TARGET_EXCHANGE_ADDRESS = "VALID_EXCHANGE_XLM_ADDRESS"
        private const val MEMO_TEXT = "ADDRESS_PART_FOR_MEMO"
        private val FEE_REGULAR = 2000.stroops()

        private val EXPECTED_AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular)
    }
}
