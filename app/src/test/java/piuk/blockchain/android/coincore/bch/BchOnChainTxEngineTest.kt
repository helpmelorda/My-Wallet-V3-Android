package piuk.blockchain.android.coincore.bch

import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.preferences.WalletStatus
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.satoshiCash
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.atMost
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.wallet.payment.OutputType
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.coincore.BlockchainAccount
import kotlin.test.assertEquals
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager

class BchOnChainTxEngineTest : CoincoreTestBase() {

    private val bchDataManager: BchDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock {
        on { getAddressOutputType(TARGET_ADDRESS) }.thenReturn(OutputType.P2PKH)
        on { getXpubFormatOutputType(XPub.Format.LEGACY) }.thenReturn(OutputType.P2PKH)
    }
    private val sendDataManager: SendDataManager = mock()

    private val bchFeeOptions: FeeOptions = mock {
        on { regularFee }.thenReturn(FEE_REGULAR)
        on { priorityFee }.thenReturn(FEE_PRIORITY)
    }
    private val feeManager: FeeDataManager = mock {
        on { bchFeeOptions }.thenReturn(Observable.just(bchFeeOptions))
    }

    private val walletPreferences: WalletStatus = mock {
        on { getFeeTypeForAsset(ASSET) }.thenReturn(FeeLevel.Regular.ordinal)
    }

    private val subject = BchOnChainTxEngine(
        bchDataManager = bchDataManager,
        payloadDataManager = payloadDataManager,
        sendDataManager = sendDataManager,
        feeManager = feeManager,
        requireSecondPassword = false,
        walletPreferences = walletPreferences
    )

    @Before
    fun setup() {
        initMocks()
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: BchCryptoWalletAccount = mock {
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
        verify(txTarget, atLeastOnce()).asset
        verify(sourceAccount, atLeastOnce()).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Asset incorrect`() {
        val sourceAccount: BchCryptoWalletAccount = mock {
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
        val sourceAccount: BchCryptoWalletAccount = mock {
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
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val sourceAccount: BchCryptoWalletAccount = mock {
            on { asset }.thenReturn(ASSET)
        }

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
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
                it.amount == CryptoValue.zero(ASSET) &&
                    it.totalBalance == CryptoValue.zero(ASSET) &&
                    it.availableBalance == CryptoValue.zero(ASSET) &&
                    it.feeAmount == CryptoValue.zero(ASSET) &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.confirmations.isEmpty() &&
                    it.minLimit == null &&
                    it.maxLimit == null &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertValue {
                verifyFeeLevels(it.feeSelection, FeeLevel.Regular)
            }.assertNoErrors()
            .assertComplete()

        verify(currencyPrefs).selectedFiatCurrency
        verify(sourceAccount, atLeastOnce()).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly for regular fees`() {
        // Arrange
        val inputAmount = 2.bitcoinCash()
        val feePerKb = (FEE_REGULAR * 1000).satoshiCash()
        val totalFee = (FEE_REGULAR * 1000 * 3).satoshiCash()

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        val totalBalance = 21.bitcoinCash()
        val availableBalance = 19.bitcoinCash()
        val totalSweepable = totalBalance - totalFee

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        whenever(bchDataManager.getAddressBalance(SOURCE_XPUB)).thenReturn(totalBalance)

        val unspentOutputs = listOf<Utxo>(
            mock(), mock()
        )

        whenever(sendDataManager.getUnspentBchOutputs(SOURCE_XPUB))
            .thenReturn(Single.just(unspentOutputs))

        whenever(
            sendDataManager.getMaximumAvailable(
                ASSET,
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                feePerKb
            )
        ).thenReturn(
            SendDataManager.MaxAvailable(
                totalSweepable as CryptoValue,
                totalFee
            )
        )

        val utxoBundle: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(totalFee.toBigInteger())
        }

        whenever(sendDataManager.getSpendableCoins(
            unspentOutputs,
            TARGET_OUTPUT_TYPE,
            CHANGE_OUTPUT_TYPE,
            inputAmount,
            feePerKb
        )).thenReturn(utxoBundle)

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
                asset = CryptoCurrency.BCH
            )
        )

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertValue {
                it.amount == inputAmount &&
                it.totalBalance == totalBalance &&
                it.availableBalance == totalSweepable &&
                it.feeForFullAvailable == totalFee &&
                it.feeAmount == totalFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }
            .assertComplete()
            .assertNoErrors()

        verify(txTarget, atMost(2)).address
        verify(sourceAccount, atLeastOnce()).asset
        verify(sourceAccount).xpubAddress
        verify(sourceAccount).accountBalance
        verify(bchDataManager).getAddressBalance(SOURCE_XPUB)
        verify(feeManager).bchFeeOptions
        verify(bchFeeOptions).regularFee
        verify(sendDataManager).getUnspentBchOutputs(SOURCE_XPUB)
        verify(sendDataManager).getMaximumAvailable(ASSET, unspentOutputs, TARGET_OUTPUT_TYPE, feePerKb)
        verify(utxoBundle).absoluteFee
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKb)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from REGULAR to PRIORITY is rejected`() {
        // Arrange
        val inputAmount = 2.bitcoinCash()
        val totalFee = (FEE_REGULAR * 1000 * 3).satoshiCash()

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        val totalBalance = 21.bitcoinCash()
        val availableBalance = 19.bitcoinCash()
        val totalSweepable = totalBalance - totalFee

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalSweepable,
            feeForFullAvailable = totalFee,
            feeAmount = totalFee,
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

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from REGULAR to NONE is rejected`() {
        // Arrange
        val inputAmount = 2.bitcoinCash()
        val totalFee = (FEE_REGULAR * 1000 * 3).satoshiCash()

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        val totalBalance = 21.bitcoinCash()
        val availableBalance = 19.bitcoinCash()
        val totalSweepable = totalBalance - totalFee

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalSweepable,
            feeForFullAvailable = totalFee,
            feeAmount = totalFee,
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

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from REGULAR to CUSTOM is rejected`() {
        // Arrange
        val inputAmount = 2.bitcoinCash()
        val totalFee = (FEE_REGULAR * 1000 * 3).satoshiCash()

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        val totalBalance = 21.bitcoinCash()
        val availableBalance = 19.bitcoinCash()
        val totalSweepable = totalBalance - totalFee

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalSweepable,
            feeForFullAvailable = totalFee,
            feeAmount = totalFee,
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
            100
        ).test()

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update fee level from REGULAR to REGULAR has no effect`() {
        // Arrange
        val inputAmount = 2.bitcoinCash()
        val totalFee = (FEE_REGULAR * 1000 * 3).satoshiCash()

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        val totalBalance = 21.bitcoinCash()
        val availableBalance = 19.bitcoinCash()
        val totalSweepable = totalBalance - totalFee

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalSweepable,
            feeForFullAvailable = totalFee,
            feeAmount = totalFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.BCH
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Regular,
            -1
        ).test()
            .assertValue {
                it.amount == inputAmount &&
                it.totalBalance == totalBalance &&
                it.availableBalance == totalSweepable &&
                it.feeAmount == totalFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }
            .assertComplete()
            .assertNoErrors()

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun fundedSourceAccount(totalBalance: Money, availableBalance: Money) =
        mock<BchCryptoWalletAccount> {
            on { asset }.thenReturn(ASSET)
            on { accountBalance }.thenReturn(Single.just(totalBalance))
            on { actionableBalance }.thenReturn(Single.just(availableBalance))
            on { xpubAddress }.thenReturn(SOURCE_XPUB)
        }

    private fun verifyFeeLevels(feeSelection: FeeSelection, expectedLevel: FeeLevel) =
        feeSelection.selectedLevel == expectedLevel &&
            feeSelection.availableLevels == EXPECTED_AVAILABLE_FEE_LEVELS &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.customAmount == -1L &&
            feeSelection.asset == CryptoCurrency.BCH

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(bchDataManager)
        verifyNoMoreInteractions(sendDataManager)
        verifyNoMoreInteractions(feeManager)
        verifyNoMoreInteractions(bchFeeOptions)
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private val ASSET = CryptoCurrency.BCH
        private val WRONG_ASSET = CryptoCurrency.ETHER
        private const val SOURCE_XPUB = "VALID_BCH_XPUB"
        private const val TARGET_ADDRESS = "VALID_BCH_ADDRESS"
        private const val FEE_REGULAR = 5L
        private const val FEE_PRIORITY = 11L

        private val TARGET_OUTPUT_TYPE = OutputType.P2PKH
        private val CHANGE_OUTPUT_TYPE = OutputType.P2PKH

        private val EXPECTED_AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular)
    }
}
