package piuk.blockchain.android.coincore.impl

import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.service.TierService
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TransferError
import piuk.blockchain.android.coincore.btc.BtcOnChainTxEngine
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.coincore.impl.txEngine.BitpayTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.FiatDepositTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.FiatWithdrawalTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.TradingToOnChainTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine
import piuk.blockchain.android.coincore.impl.txEngine.interest.InterestDepositOnChainTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.interest.InterestDepositTradingEngine
import piuk.blockchain.android.coincore.impl.txEngine.interest.InterestWithdrawOnChainTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.interest.InterestWithdrawTradingTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.sell.OnChainSellTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.sell.TradingSellTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.swap.OnChainSwapTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.swap.TradingToTradingSwapTxEngine
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase.Companion.SECONDARY_TEST_ASSET
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase.Companion.TEST_ASSET
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.simplebuy.BankPartnerCallbackProvider
import java.lang.IllegalStateException

class TxProcessorFactoryTest {

    private val bitPayManager: BitPayDataManager = mock()
    private val exchangeRates: ExchangeRatesDataManager = mock()
    private val walletManager: CustodialWalletManager = mock()
    private val interestBalances: InterestBalanceDataManager = mock()
    private val walletPrefs: WalletStatus = mock()
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider = mock()
    private val quotesEngine: TransferQuotesEngine = mock()
    private val analytics: Analytics = mock()
    private val kycTierService: TierService = mock()

    private lateinit var subject: TxProcessorFactory

    @Before
    fun setup() {
        subject = TxProcessorFactory(
            bitPayManager = bitPayManager,
            exchangeRates = exchangeRates,
            walletManager = walletManager,
            interestBalances = interestBalances,
            walletPrefs = walletPrefs,
            bankPartnerCallbackProvider = bankPartnerCallbackProvider,
            quotesEngine = quotesEngine,
            analytics = analytics,
            kycTierService = kycTierService
        )
    }

    @Test
    fun onChainBitpayProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            // this Crypto instance needs to live here as Bitpay only accepts BTC and BCH
            on { asset }.thenReturn(CryptoCurrency.BTC)
        }

        val target: BitPayInvoiceTarget = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is BitpayTxEngine &&
                    (it.engine as BitpayTxEngine).run {
                        this.bitPayDataManager == bitPayManager &&
                            this.walletPrefs == walletPrefs &&
                            this.assetEngine == mockBaseEngine &&
                            this.analytics == analytics
                    }
            }
    }

    @Test
    fun onChainInterestProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            on { asset }.thenReturn(TEST_ASSET)
        }

        val mockReceiveAddress: ReceiveAddress = mock()
        val target: CryptoInterestAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == mockReceiveAddress &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestDepositOnChainTxEngine &&
                    (it.engine as InterestDepositOnChainTxEngine).run {
                        this.walletManager == walletManager &&
                            this.interestBalances == interestBalances &&
                            this.onChainEngine == mockBaseEngine
                    }
            }
    }

    @Test
    fun onChainAddressProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: CryptoAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine == mockBaseEngine
            }
    }

    @Test
    fun onChainCryptoAccountSwapActionProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: CryptoNonCustodialAccount = mock {
            on { asset }.thenReturn(SECONDARY_TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Swap)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is OnChainSwapTxEngine &&
                    (it.engine as OnChainSwapTxEngine).run {
                        this.walletManager == walletManager &&
                            this.kycTierService == kycTierService &&
                            this.engine == mockBaseEngine
                    }
            }
    }

    @Test
    fun onChainCryptoAccountOtherActionProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            on { asset }.thenReturn(TEST_ASSET)
        }

        val mockReceiveAddress: ReceiveAddress = mock()
        val target: CryptoAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == mockReceiveAddress &&
                    it.exchangeRates == exchangeRates &&
                    it.engine == mockBaseEngine
            }
    }

    @Test
    fun onChainFiatAccountProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: FiatAccount = mock {
            on { fiatCurrency }.thenReturn("EUR")
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is OnChainSellTxEngine &&
                    (it.engine as OnChainSellTxEngine).run {
                        this.engine == mockBaseEngine &&
                            this.walletManager == walletManager &&
                            this.kycTierService == kycTierService
                    }
            }
    }

    @Test
    fun onChainToUnknownProcessor() {
        val source: CryptoNonCustodialAccount = mock()
        val target: LinkedBankAccount.BankAccountAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertError {
                it is TransferError
            }
    }

    @Test
    fun tradingToOnChainNoteSupportedSendProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
            on { isNoteSupported }.thenReturn(true)
        }

        val target: CryptoAddress = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingToOnChainTxEngine &&
                    (it.engine as TradingToOnChainTxEngine).run {
                        this.isNoteSupported == source.isNoteSupported &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToOnChainNoteNotSupportedSendProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
            on { isNoteSupported }.thenReturn(false)
        }

        val target: CryptoAddress = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingToOnChainTxEngine &&
                    (it.engine as TradingToOnChainTxEngine).run {
                        this.isNoteSupported == source.isNoteSupported &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToInterestProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: CryptoInterestAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestDepositTradingEngine &&
                    (it.engine as InterestDepositTradingEngine).run {
                        this.interestBalances == interestBalances &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToFiatProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: FiatAccount = mock {
            on { fiatCurrency }.thenReturn("EUR")
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingSellTxEngine &&
                    (it.engine as TradingSellTxEngine).run {
                        this.kycTierService == kycTierService &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToTradingProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: CustodialTradingAccount = mock {
            on { asset }.thenReturn(SECONDARY_TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingToTradingSwapTxEngine &&
                    (it.engine as TradingToTradingSwapTxEngine).run {
                        this.kycTierService == kycTierService &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToOnChainNoteSupportedProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
            on { isNoteSupported }.thenReturn(true)
        }

        val mockReceiveAddress: CryptoAddress = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }
        val target: CryptoAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == mockReceiveAddress &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingToOnChainTxEngine &&
                    (it.engine as TradingToOnChainTxEngine).run {
                        this.isNoteSupported == source.isNoteSupported &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToOnChainNoteNotSupportedProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
            on { isNoteSupported }.thenReturn(false)
        }

        val mockReceiveAddress: CryptoAddress = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }
        val target: CryptoAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == mockReceiveAddress &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingToOnChainTxEngine &&
                    (it.engine as TradingToOnChainTxEngine).run {
                        this.isNoteSupported == source.isNoteSupported &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToUnknownProcessor() {
        val source: CustodialTradingAccount = mock()
        val target: LinkedBankAccount.BankAccountAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertError {
                it is TransferError
            }
    }

    @Test
    fun interestWithdrawalToTradingProcessor() {
        val source: CryptoInterestAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }
        val target: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestWithdrawTradingTxEngine &&
                    (it.engine as InterestWithdrawTradingTxEngine).run {
                        this.walletManager == walletManager &&
                            this.interestBalances == interestBalances
                    }
            }
    }

    @Test
    fun interestWithdrawalToOnChainProcessor() {
        val source: CryptoInterestAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }
        val target: CryptoNonCustodialAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestWithdrawOnChainTxEngine &&
                    (it.engine as InterestWithdrawOnChainTxEngine).run {
                        this.walletManager == walletManager &&
                            this.interestBalances == interestBalances
                    }
            }
    }

    @Test
    fun interestWithdrawalUnknownProcessor() {
        val source: CryptoInterestAccount = mock()
        val target: LinkedBankAccount.BankAccountAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertError {
                it is IllegalStateException
            }
    }

    @Test
    fun fiatDepositProcessor() {
        val source: LinkedBankAccount = mock()
        val target: FiatAccount = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is FiatDepositTxEngine &&
                    (it.engine as FiatDepositTxEngine).run {
                        this.walletManager == walletManager &&
                            this.bankPartnerCallbackProvider == bankPartnerCallbackProvider
                    }
            }
    }

    @Test
    fun fiatDepositUnknownProcessor() {
        val source: LinkedBankAccount = mock()
        val target: LinkedBankAccount.BankAccountAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertError {
                it is IllegalStateException
            }
    }

    @Test
    fun fiatWithdrawalProcessor() {
        val source: FiatAccount = mock()
        val target: LinkedBankAccount = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is FiatWithdrawalTxEngine &&
                    (it.engine as FiatWithdrawalTxEngine).run {
                        this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun fiatWithdrawalUnknownProcessor() {
        val source: FiatAccount = mock()
        val target: LinkedBankAccount.BankAccountAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertError {
                it is IllegalStateException
            }
    }
}