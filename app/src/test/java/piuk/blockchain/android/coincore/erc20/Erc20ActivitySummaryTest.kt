package piuk.blockchain.android.coincore.erc20

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.erc20.model.Erc20HistoryEvent
import com.blockchain.core.price.ExchangeRatesDataManager
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class Erc20ActivitySummaryTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val erc20DataManager: Erc20DataManager = mock()
    private val exchangeRates: ExchangeRatesDataManager = mock()

    @Test
    fun transactionMapsToReceive() {
        val fromAccount = "0x4058a004dd718babab47e14dd0d744742e5b9903"
        val toAccount = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29"
        val erc20HistoryEvent = Erc20HistoryEvent(
            from = fromAccount,
            to = toAccount,
            value = CryptoValue.fromMinor(ERC20_TOKEN, 10000.toBigInteger()),
            transactionHash = TX_HASH,
            blockNumber = 7721219.toBigInteger(),
            timestamp = 1557334297,
            fee = Single.just(CryptoValue.Companion.fromMinor(ERC20_TOKEN, 400L.toBigInteger()))
        )

        val subject = Erc20ActivitySummaryItem(
            asset = ERC20_TOKEN,
            event = erc20HistoryEvent,
            accountHash = toAccount,
            erc20DataManager = erc20DataManager,
            exchangeRates = exchangeRates,
            lastBlockNumber = 100.toBigInteger(),
            account = mock()
        )

        assertEquals(TransactionSummary.TransactionType.RECEIVED, subject.transactionType)
    }

    @Test
    fun transactionMapsToTransferred() {
        val fromAccount = "0x4058a004dd718babab47e14dd0d744742e5b9903"

        val erc20HistoryEvent = Erc20HistoryEvent(
            from = fromAccount,
            to = fromAccount,
            value = CryptoValue.fromMinor(ERC20_TOKEN, 10000.toBigInteger()),
            transactionHash = TX_HASH,
            blockNumber = 7721219.toBigInteger(),
            timestamp = 1557334297,
            fee = Single.just(CryptoValue.Companion.fromMinor(ERC20_TOKEN, 400L.toBigInteger()))
        )

        val subject = Erc20ActivitySummaryItem(
            asset = ERC20_TOKEN,
            event = erc20HistoryEvent,
            accountHash = fromAccount,
            erc20DataManager = erc20DataManager,
            exchangeRates = exchangeRates,
            lastBlockNumber = 100.toBigInteger(),
            account = mock()
        )

        assertEquals(TransactionSummary.TransactionType.TRANSFERRED, subject.transactionType)
    }

    @Test
    fun transactionMapsToSent() {
        val fromAccount = "0x4058a004dd718babab47e14dd0d744742e5b9903"
        val toAccount = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29"
        val erc20HistoryEvent = Erc20HistoryEvent(
            from = fromAccount,
            to = toAccount,
            value = CryptoValue.fromMinor(ERC20_TOKEN, 10000.toBigInteger()),
            transactionHash = TX_HASH,
            blockNumber = 7721219.toBigInteger(),
            timestamp = 1557334297,
            fee = Single.just(CryptoValue.Companion.fromMinor(ERC20_TOKEN, 400L.toBigInteger()))
        )

        val subject = Erc20ActivitySummaryItem(
            asset = ERC20_TOKEN,
            event = erc20HistoryEvent,
            accountHash = fromAccount,
            erc20DataManager = erc20DataManager,
            exchangeRates = exchangeRates,
            lastBlockNumber = 100.toBigInteger(),
            account = mock()
        )

        assertEquals(TransactionSummary.TransactionType.SENT, subject.transactionType)
    }

    @Test
    fun descriptionIsFetchedFromDatamanager() {
        val description = "This is a transaction note"

        whenever(erc20DataManager.getErc20TxNote(ERC20_TOKEN, TX_HASH)).thenReturn(description)

        val fromAccount = "0x4058a004dd718babab47e14dd0d744742e5b9903"
        val toAccount = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29"
        val erc20HistoryEvent = Erc20HistoryEvent(
            from = fromAccount,
            to = toAccount,
            value = CryptoValue.fromMinor(ERC20_TOKEN, 10000.toBigInteger()),
            transactionHash = TX_HASH,
            blockNumber = 7721219.toBigInteger(),
            timestamp = 1557334297,
            fee = Single.just(CryptoValue.Companion.fromMinor(ERC20_TOKEN, 400L.toBigInteger()))
        )

        val subject = Erc20ActivitySummaryItem(
            asset = ERC20_TOKEN,
            event = erc20HistoryEvent,
            accountHash = fromAccount,
            erc20DataManager = erc20DataManager,
            exchangeRates = exchangeRates,
            lastBlockNumber = 100.toBigInteger(),
            account = mock()
        )

        val result = subject.description

        assertEquals(description, result)

        verify(erc20DataManager).getErc20TxNote(asset = ERC20_TOKEN, txHash = TX_HASH)
        verifyNoMoreInteractions(erc20DataManager)
    }

    @Test
    fun descriptionIsUpdatedToDatamanager() {
        val description = "This is a transaction note"

        whenever(
            erc20DataManager.putErc20TxNote(
                asset = ERC20_TOKEN,
                txHash = TX_HASH,
                note = description
            )
        ).thenReturn(Completable.complete())

        val fromAccount = "0x4058a004dd718babab47e14dd0d744742e5b9903"
        val toAccount = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29"
        val erc20HistoryEvent = Erc20HistoryEvent(
            from = fromAccount,
            to = toAccount,
            value = CryptoValue.fromMinor(ERC20_TOKEN, 10000.toBigInteger()),
            transactionHash = TX_HASH,
            blockNumber = 7721219.toBigInteger(),
            timestamp = 1557334297,
            fee = Single.just(CryptoValue.Companion.fromMinor(ERC20_TOKEN, 400L.toBigInteger()))
        )

        val subject = Erc20ActivitySummaryItem(
            asset = ERC20_TOKEN,
            event = erc20HistoryEvent,
            accountHash = fromAccount,
            erc20DataManager = erc20DataManager,
            exchangeRates = exchangeRates,
            lastBlockNumber = 100.toBigInteger(),
            account = mock()
        )

        subject.updateDescription(description).test().assertComplete()

        verify(erc20DataManager)
            .putErc20TxNote(
                asset = ERC20_TOKEN,
                txHash = TX_HASH,
                note = description
            )

        verifyNoMoreInteractions(erc20DataManager)
    }

    companion object {
        private const val TX_HASH = "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff"

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
