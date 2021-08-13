package piuk.blockchain.android.coincore.erc20

import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.erc20.model.Erc20HistoryEvent
import com.blockchain.preferences.WalletStatus
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.repositories.swap.TradeTransactionItem
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.coincore.impl.CryptoAccountBase
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class Erc20AccountActivityTest : CoincoreTestBase() {

    private val payloadManager: PayloadDataManager = mock()
    private val erc20DataManager: Erc20DataManager = mock()

    private val walletPreferences: WalletStatus = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()

    private val subject = Erc20NonCustodialAccount(
        asset = ERC20_TOKEN,
        payloadManager = payloadManager,
        label = "Text Dgld Account",
        address = FROM_ADDRESS,
        fees = mock(),
        erc20DataManager = erc20DataManager,
        exchangeRates = exchangeRates,
        walletPreferences = walletPreferences,
        custodialWalletManager = custodialWalletManager,
        identity = mock(),
        baseActions = CryptoAccountBase.defaultActions
    )

    @Before
    fun setup() {
        initMocks()
    }

    @Test
    fun getErc20TransactionsList() {
        val erc20HistoryEvent = Erc20HistoryEvent(
            from = FROM_ADDRESS,
            to = TO_ADDRESS,
            value = CryptoValue.fromMinor(ERC20_TOKEN, 10000.toBigInteger()),
            transactionHash = "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff",
            blockNumber = 7721219.toBigInteger(),
            timestamp = 1557334297,
            fee = Single.just(CryptoValue.Companion.fromMinor(ERC20_TOKEN, 400L.toBigInteger()))
        )

        val erc20HistoryList = listOf(erc20HistoryEvent)

        val swapSummary = TradeTransactionItem(
            txId = "123",
            timeStampMs = 1L,
            direction = TransferDirection.ON_CHAIN,
            sendingAddress = "sendingAddress",
            receivingAddress = "receivingAddress",
            state = CustodialOrderState.FINISHED,
            sendingValue = CryptoValue.zero(ERC20_TOKEN),
            receivingValue = CryptoValue.zero(CryptoCurrency.BTC),
            withdrawalNetworkFee = CryptoValue.zero(CryptoCurrency.BTC),
            currencyPair = CurrencyPair.CryptoCurrencyPair(ERC20_TOKEN, CryptoCurrency.BTC),
            apiFiatValue = FiatValue.zero("USD")
        )

        val summaryList = listOf(swapSummary)

        whenever(erc20DataManager.getErc20History(ERC20_TOKEN))
            .thenReturn(Single.just(erc20HistoryList))

        whenever(erc20DataManager.latestBlockNumber())
            .thenReturn(
                Single.just(erc20HistoryEvent.blockNumber.plus(3.toBigInteger()))
            )

        whenever(custodialWalletManager.getCustodialActivityForAsset(any(), any()))
            .thenReturn(Single.just(summaryList))

        subject.activity
            .test()
            .assertValueCount(1)
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.size == 1 && it[0].run {
                    this is Erc20ActivitySummaryItem &&
                        asset == ERC20_TOKEN &&
                        !doubleSpend &&
                        !isFeeTransaction &&
                        confirmations == 3 &&
                        timeStampMs == 1557334297000L &&
                        transactionType == TransactionSummary.TransactionType.SENT &&
                        txId == "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff" &&
                        confirmations == 3 &&
                        value == CryptoValue.fromMinor(ERC20_TOKEN, 10000.toBigInteger()) &&
                        inputsMap[FROM_ADDRESS] == CryptoValue.fromMinor(ERC20_TOKEN, 10000.toBigInteger()) &&
                        outputsMap[TO_ADDRESS] == CryptoValue.fromMinor(ERC20_TOKEN, 10000.toBigInteger())
                }
            }

        verify(erc20DataManager).getErc20History(ERC20_TOKEN)
    }

    companion object {
        private const val FROM_ADDRESS = "0x4058a004dd718babab47e14dd0d744742e5b9903"
        private const val TO_ADDRESS = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29"

        @Suppress("ClassName")
        private object ERC20_TOKEN : CryptoCurrency(
            ticker = "DUMMY",
            name = "Dummies",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        )
    }
}
