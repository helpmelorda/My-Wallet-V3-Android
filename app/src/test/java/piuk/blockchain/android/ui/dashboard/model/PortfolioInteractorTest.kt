package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.android.testutils.rxInit
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.BankPartner
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.nabu.models.data.YodleeAttributes
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.coincore.fiat.LinkedBanksFactory
import piuk.blockchain.android.ui.settings.LinkablePaymentMethods

class PortfolioInteractorTest {

    private lateinit var interactor: PortfolioInteractor
    private val linkedBanksFactory: LinkedBanksFactory = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val model: PortfolioModel = mock()
    private val targetFiatAccount: FiatAccount = mock {
        on { fiatCurrency }.thenReturn("USD")
    }

    private val internalFlags: InternalFeatureFlagApi = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
        mainTrampoline()
    }

    @Before
    fun setUp() {
        interactor = PortfolioInteractor(
            coincore = mock(),
            payloadManager = mock(),
            exchangeRates = mock(),
            currencyPrefs = mock(),
            custodialWalletManager = custodialWalletManager,
            linkedBanksFactory = linkedBanksFactory,
            crashLogger = mock(),
            analytics = mock(),
            simpleBuyPrefs = mock(),
            gatedFeatures = mock()
        )
    }

    @Test
    fun `for both available methods with no available bank transfer banks, chooser should be triggered`() {
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_TRANSFER, PaymentMethodType.BANK_ACCOUNT)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                emptyList()
            )
        )

        whenever(internalFlags.isFeatureEnabled(any())).thenReturn(true)

        interactor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false
        )

        verify(model).process(
            ShowLinkablePaymentMethodsSheet(
                LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit(
                    LinkablePaymentMethods(
                        "USD",
                        listOf(
                            PaymentMethodType.BANK_TRANSFER, PaymentMethodType.BANK_ACCOUNT
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `for only bank transfer available with no available bank transfer banks, bank link should launched`() {
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_TRANSFER)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                emptyList()
            )
        )
        whenever(custodialWalletManager.linkToABank("USD")).thenReturn(
            Single.just(
                LinkBankTransfer(
                    "123", BankPartner.YODLEE, YodleeAttributes("", "", "")
                )
            )
        )
        whenever(internalFlags.isFeatureEnabled(any())).thenReturn(true)

        interactor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false
        )

        verify(model).process(
            LaunchBankLinkFlow(
                LinkBankTransfer(
                    "123", BankPartner.YODLEE, YodleeAttributes("", "", "")
                ),
                AssetAction.FiatDeposit
            )
        )
    }

    @Test
    fun `for only funds with no available bank transfer banks, wire transfer should launched`() {
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_ACCOUNT)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                emptyList()
            )
        )

        interactor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false
        )

        verify(model).process(
            ShowBankLinkingSheet(targetFiatAccount)
        )
    }

    @Test
    fun `with 1 available bank transfer, flow should be launched and wire transfer should get ignored`() {
        val linkedBankAccount: LinkedBankAccount = mock {
            on { currency }.thenReturn("USD")
        }
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_ACCOUNT, PaymentMethodType.BANK_TRANSFER)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                listOf(
                    linkedBankAccount
                )
            )
        )

        whenever(internalFlags.isFeatureEnabled(any())).thenReturn(true)

        interactor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false
        )

        verify(model).process(any<UpdateLaunchDialogFlow>())
    }

    @Test
    fun `if linked bank should launched then wire transfer should get ignored and link bank should be launched`() {
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_ACCOUNT, PaymentMethodType.BANK_TRANSFER)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                emptyList()
            )
        )

        whenever(custodialWalletManager.linkToABank("USD")).thenReturn(
            Single.just(
                LinkBankTransfer(
                    "123", BankPartner.YODLEE, YodleeAttributes("", "", "")
                )
            )
        )

        whenever(internalFlags.isFeatureEnabled(any())).thenReturn(true)

        interactor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = true
        )

        verify(model).process(
            LaunchBankLinkFlow(
                LinkBankTransfer(
                    "123", BankPartner.YODLEE, YodleeAttributes("", "", "")
                ),
                AssetAction.FiatDeposit
            )
        )
    }
}