package piuk.blockchain.android.coincore.impl.txEngine.swap

import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.datamanagers.TransferQuote
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.NabuApiException
import com.blockchain.nabu.models.responses.nabu.NabuErrorCodes
import com.blockchain.nabu.service.TierService
import com.blockchain.testutils.bitcoin
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.btc.BtcCryptoWalletAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.coincore.impl.txEngine.PricedQuote
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase
import piuk.blockchain.android.coincore.xlm.XlmCryptoWalletAccount
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import kotlin.test.assertEquals

class TradingToTradingSwapTxEngineTest : CoincoreTestBase() {

    private val walletManager: CustodialWalletManager = mock()
    private val quotesEngine: TransferQuotesEngine = mock()
    private val kycTierService: TierService = mock()
    private val environmentConfig: EnvironmentConfig = mock()

    private val subject = TradingToTradingSwapTxEngine(
        walletManager = walletManager,
        quotesEngine = quotesEngine,
        kycTierService = kycTierService
    )

    @Before
    fun setup() {
        initMocks()

        whenever(exchangeRates.getLastCryptoToUserFiatRate(SRC_ASSET))
            .thenReturn(
                ExchangeRate.CryptoToFiat(
                    from = SRC_ASSET,
                    to = TEST_USER_FIAT,
                    rate = EXCHANGE_RATE
                )
            )
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: CustodialTradingAccount = mock {
            on { asset }.thenReturn(SRC_ASSET)
        }

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).asset
        verifyQuotesEngineStarted()

        noMoreInteractions(txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Account incorrect`() {
        val sourceAccount: BtcCryptoWalletAccount = mock {
            on { asset }.thenReturn(SRC_ASSET)
        }

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
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
    fun `inputs fail validation when assets match`() {
        val sourceAccount: CustodialTradingAccount = mock {
            on { asset }.thenReturn(SRC_ASSET)
        }

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(SRC_ASSET)
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
    fun `inputs fail validation when target incorrect`() {
        val sourceAccount: CustodialTradingAccount = mock {
            on { asset }.thenReturn(SRC_ASSET)
        }

        val txTarget: XlmCryptoWalletAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
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
        val sourceAccount: CustodialTradingAccount = mock {
            on { asset }.thenReturn(SRC_ASSET)
        }

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        assertEquals(asset, SRC_ASSET)

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).asset
        verifyQuotesEngineStarted()

        noMoreInteractions(txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        whenUserIsGold()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
        }

        val txQuote: TransferQuote = mock {
            on { sampleDepositAddress }.thenReturn(SAMPLE_DEPOSIT_ADDRESS)
            on { networkFee }.thenReturn(NETWORK_FEE)
        }

        val pricedQuote: PricedQuote = mock {
            on { transferQuote }.thenReturn(txQuote)
            on { price }.thenReturn(INITIAL_QUOTE_PRICE)
        }

        whenever(quotesEngine.pricedQuote).thenReturn(Observable.just(pricedQuote))
        whenever(quotesEngine.getLatestQuote()).thenReturn(pricedQuote)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val minForFee = NETWORK_FEE.toBigDecimal().divide(INITIAL_QUOTE_PRICE.toBigDecimal())
        val expectedMinLimit = MIN_GOLD_LIMIT_ASSET + CryptoValue.fromMajor(SRC_ASSET, minForFee)

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(SRC_ASSET) &&
                it.totalBalance == totalBalance &&
                it.availableBalance == totalBalance &&
                it.feeAmount == CryptoValue.zero(SRC_ASSET) &&
                it.selectedFiat == TEST_USER_FIAT &&
                it.confirmations.isEmpty() &&
                it.minLimit == expectedMinLimit &&
                it.maxLimit == MAX_GOLD_LIMIT_ASSET &&
                it.validationState == ValidationState.UNINITIALISED &&
                it.engineState[USER_TIER] != null
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).asset
        verify(currencyPrefs).selectedFiatCurrency
        verifyQuotesEngineStarted()
        verifyLimitsFetched()
        verify(quotesEngine).pricedQuote
        verify(quotesEngine, atLeastOnce()).getLatestQuote()
        verify(exchangeRates).getLastCryptoToUserFiatRate(SRC_ASSET)

        noMoreInteractions(txTarget)
    }

    @Test
    fun `PendingTx initialisation when limit reached`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
        }

        val error: NabuApiException = mock {
            on { getErrorCode() }.thenReturn(NabuErrorCodes.PendingOrdersLimitReached)
        }

        whenever(quotesEngine.pricedQuote).thenReturn(Observable.error(error))

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(SRC_ASSET) &&
                it.totalBalance == CryptoValue.zero(SRC_ASSET) &&
                it.availableBalance == CryptoValue.zero(SRC_ASSET) &&
                it.feeForFullAvailable == CryptoValue.zero(SRC_ASSET) &&
                it.feeAmount == CryptoValue.zero(SRC_ASSET) &&
                it.selectedFiat == TEST_USER_FIAT &&
                it.confirmations.isEmpty() &&
                it.minLimit == null &&
                it.maxLimit == null &&
                it.validationState == ValidationState.PENDING_ORDERS_LIMIT_REACHED &&
                it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).asset
        verify(currencyPrefs).selectedFiatCurrency
        verifyQuotesEngineStarted()
        verify(quotesEngine).pricedQuote

        noMoreInteractions(txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(SRC_ASSET),
            totalBalance = CryptoValue.zero(SRC_ASSET),
            availableBalance = CryptoValue.zero(SRC_ASSET),
            feeForFullAvailable = CryptoValue.zero(SRC_ASSET),
            feeAmount = CryptoValue.zero(SRC_ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        val inputAmount = 2.bitcoin()
        val expectedFee = 0.bitcoin()

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == totalBalance &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertComplete()
            .assertNoErrors()

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).asset
        verifyQuotesEngineStarted()

        verify(quotesEngine).updateAmount(inputAmount)

        noMoreInteractions(txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to PRIORITY is rejected`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalBalance,
            feeForFullAvailable = zeroBtc,
            feeAmount = zeroBtc,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Priority,
            -1
        ).test()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to REGULAR is rejected`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalBalance,
            feeForFullAvailable = zeroBtc,
            feeAmount = zeroBtc,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Regular,
            -1
        ).test()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to CUSTOM is rejected`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalBalance,
            feeForFullAvailable = zeroBtc,
            feeAmount = zeroBtc,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Custom,
            100
        ).test()
    }

    @Test
    fun `update fee level from NONE to NONE has no effect`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalBalance,
            feeForFullAvailable = zeroBtc,
            feeAmount = zeroBtc,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.None,
            -1
        ).test()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == totalBalance &&
                    it.feeAmount == zeroBtc
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertComplete()
            .assertNoErrors()

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).asset
        verifyQuotesEngineStarted()

        noMoreInteractions(txTarget)
    }

    private fun fundedSourceAccount(totalBalance: Money, availableBalance: Money) =
        mock<CustodialTradingAccount> {
            on { asset }.thenReturn(SRC_ASSET)
            on { accountBalance }.thenReturn(Single.just(totalBalance))
            on { actionableBalance }.thenReturn(Single.just(availableBalance))
        }

    private fun whenUserIsGold() {
        val kycTiers: KycTiers = mock()
        whenever(kycTierService.tiers()).thenReturn(Single.just(kycTiers))

        whenever(walletManager.getProductTransferLimits(TEST_USER_FIAT, Product.TRADE, TransferDirection.INTERNAL))
            .thenReturn(
                Single.just(
                    TransferLimits(
                        minLimit = MIN_GOLD_LIMIT,
                        maxOrder = MAX_GOLD_ORDER,
                        maxLimit = MAX_GOLD_LIMIT
                    )
                )
            )
    }

    private fun verifyLimitsFetched() {
        verify(kycTierService).tiers()
        verify(walletManager).getProductTransferLimits(TEST_USER_FIAT, Product.TRADE, TransferDirection.INTERNAL)
    }

    private fun verifyQuotesEngineStarted() {
        verify(quotesEngine).start(
            TransferDirection.INTERNAL,
            CurrencyPair.CryptoCurrencyPair(SRC_ASSET, TGT_ASSET)
        )
    }

    private fun verifyFeeLevels(
        feeSelection: FeeSelection
    ) = feeSelection.selectedLevel == FeeLevel.None &&
        feeSelection.availableLevels == setOf(FeeLevel.None) &&
        feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
        feeSelection.asset == null &&
        feeSelection.customAmount == -1L

    private fun noMoreInteractions(txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(walletManager)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(quotesEngine)
        verifyNoMoreInteractions(kycTierService)
        verifyNoMoreInteractions(environmentConfig)
    }

    companion object {
        private val SRC_ASSET = CryptoCurrency.BTC
        private val TGT_ASSET = CryptoCurrency.XLM
        private val EXCHANGE_RATE = 2.toBigDecimal() // 1 btc == 2 INR

        private const val SAMPLE_DEPOSIT_ADDRESS = "initial quote deposit address"

        private val NETWORK_FEE = CryptoValue.fromMajor(CryptoCurrency.BTC, 0.1.toBigDecimal())

        private val INITIAL_QUOTE_PRICE = CryptoValue.fromMajor(CryptoCurrency.BTC, 10.toBigDecimal())

        private val MIN_GOLD_LIMIT = FiatValue.fromMajor(TEST_USER_FIAT, 100.toBigDecimal())
        private val MAX_GOLD_ORDER = FiatValue.fromMajor(TEST_USER_FIAT, 500.toBigDecimal())
        private val MAX_GOLD_LIMIT = FiatValue.fromMajor(TEST_USER_FIAT, 2000.toBigDecimal())

        private val MIN_GOLD_LIMIT_ASSET = CryptoValue.fromMajor(SRC_ASSET, 50.toBigDecimal())
        private val MAX_GOLD_ORDER_ASSET = CryptoValue.fromMajor(SRC_ASSET, 250.toBigDecimal())
        private val MAX_GOLD_LIMIT_ASSET = CryptoValue.fromMajor(SRC_ASSET, 1000.toBigDecimal())
    }
}
