package piuk.blockchain.android.coincore.impl

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AssetAction

class CustodialTradingAccountTest {

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val testAsset = CryptoCurrency(
        ticker = "NOPE",
        name = "Not a real thing",
        categories = setOf(AssetCategory.CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 3,
        colour = "000000"
    )

    private val exchangeRates: ExchangeRatesDataManager = mock()
    private val custodialManager: CustodialWalletManager = mock()
    private val tradingBalances: TradingBalanceDataManager = mock()
    private val identity: UserIdentity = mock()
    private val features: InternalFeatureFlagApi = mock()

    @Test
    fun `If no base Actions set then action set is empty`() {

        // Arrange
        val subject = configureActionSubject(emptySet())

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            simpleBuy = true,
            interest = true,
            supportedFiat = listOf("USD")
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it.isEmpty()
            }
    }

    @Test
    fun `All actions are available`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            simpleBuy = true,
            interest = true,
            supportedFiat = listOf("USD")
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == SUPPORTED_CUSTODIAL_ACTIONS
            }
    }

    @Test
    fun `If user has no balance and all default Actions set then action set is correct`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.zero(testAsset),
            actionableBalance = CryptoValue.zero(testAsset),
            simpleBuy = true,
            interest = true,
            supportedFiat = listOf("USD")
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.Receive,
                    AssetAction.Buy
                )
            }
    }

    @Test
    fun `If user has balance and all default Actions, but no simple buy then action set is correct`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            simpleBuy = false,
            interest = true,
            supportedFiat = listOf("USD")
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.InterestDeposit,
                    AssetAction.Receive,
                    AssetAction.Buy,
                    AssetAction.Send
                )
            }
    }

    @Test
    fun `If user has actionable balance and all default Actions then action set is correct`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            actionableBalance = CryptoValue.zero(testAsset),
            simpleBuy = true,
            interest = true,
            supportedFiat = listOf("USD")
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.InterestDeposit,
                    AssetAction.Swap,
                    AssetAction.Sell,
                    AssetAction.Receive,
                    AssetAction.Buy
                )
            }
    }

    @Test
    fun `If user has balance and all default Actions, but interest is disabled then action set is correct`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            actionableBalance = CryptoValue.zero(testAsset),
            simpleBuy = true,
            interest = false,
            supportedFiat = listOf("USD")
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.Swap,
                    AssetAction.Sell,
                    AssetAction.Receive,
                    AssetAction.Buy
                )
            }
    }

    @Test
    fun `If user has actionable balance and all default Actions, but no fiat then action set is correct`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            simpleBuy = true,
            interest = true,
            supportedFiat = emptyList()
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.InterestDeposit,
                    AssetAction.Swap,
                    AssetAction.Buy,
                    AssetAction.Receive,
                    AssetAction.Send
                )
            }
    }

    @Test
    fun `Unsupported actions are ignored`() {

        // Arrange
        val subject = configureActionSubject(
            setOf(
                AssetAction.ViewActivity,
                AssetAction.Send,
                AssetAction.ViewStatement
            )
        )

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(testAsset, 1000.toBigInteger()),
            simpleBuy = true,
            interest = true,
            supportedFiat = listOf("USD")
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.Send
                )
            }
    }

    private fun configureActionSubject(actions: Set<AssetAction>): CustodialTradingAccount =
        CustodialTradingAccount(
            asset = testAsset,
            label = "Test Account",
            exchangeRates = exchangeRates,
            custodialWalletManager = custodialManager,
            tradingBalances = tradingBalances,
            identity = identity,
            features = features,
            baseActions = actions
        )

    private fun configureActionTest(
        accountBalance: CryptoValue,
        actionableBalance: CryptoValue,
        simpleBuy: Boolean,
        interest: Boolean,
        supportedFiat: List<String>
    ) {
        whenever(identity.isEligibleFor(Feature.SimpleBuy)).thenReturn(Single.just(simpleBuy))
        val interestFeature = Feature.Interest(testAsset)
        whenever(identity.isEligibleFor(interestFeature)).thenReturn(Single.just(interest))

        whenever(tradingBalances.getTotalBalanceForAsset(testAsset))
            .thenReturn(Maybe.just(accountBalance))
        whenever(tradingBalances.getActionableBalanceForAsset(testAsset))
            .thenReturn(Maybe.just(actionableBalance))
        whenever(custodialManager.getSupportedFundsFiats())
            .thenReturn(Single.just(supportedFiat))
    }

    companion object {
        private val SUPPORTED_CUSTODIAL_ACTIONS = setOf(
            AssetAction.ViewActivity,
            AssetAction.Send,
            AssetAction.InterestDeposit,
            AssetAction.Swap,
            AssetAction.Sell,
            AssetAction.Receive,
            AssetAction.Buy
        )
    }
}
