package com.blockchain.core.chains.erc20

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.chains.erc20.call.Erc20BalanceCallCache
import com.blockchain.core.chains.erc20.call.Erc20HistoryCallCache
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import com.blockchain.core.chains.erc20.model.Erc20Balance
import com.blockchain.core.chains.erc20.model.Erc20HistoryEvent
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.Erc20TokenData
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import io.reactivex.rxjava3.core.Completable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import java.math.BigInteger

class Erc20DataManagerTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    private val ethDataManager: EthDataManager = mock {
        on { accountAddress }.thenReturn(ACCOUNT_HASH)
    }

    private val balanceCallCache: Erc20BalanceCallCache = mock()
    private val historyCallCache: Erc20HistoryCallCache = mock()

    private val subject = Erc20DataManagerImpl(
        ethDataManager = ethDataManager,
        balanceCallCache = balanceCallCache,
        historyCallCache = historyCallCache
    )

    @Test
    fun `accountHash fetches from eth data manager`() {
        val result = subject.accountHash

        assertEquals(ACCOUNT_HASH, result)

        verify(ethDataManager).accountAddress
        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `requireSecondPassword delegates to eth manager`() {
        whenever(ethDataManager.requireSecondPassword).thenReturn(true)

        val result = subject.requireSecondPassword

        assertEquals(true, result)

        verify(ethDataManager).requireSecondPassword

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `eth balance is fetched from eth data manager`() {

        val ethBalance = 1001.toBigInteger()
        val mockEthResult: CombinedEthModel = mock {
            on { getTotalBalance() }.thenReturn(ethBalance)
        }

        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(mockEthResult))

        val expectedResult = CryptoValue.fromMinor(CryptoCurrency.ETHER, ethBalance)
        subject.getEthBalance()
            .test()
            .assertValue(expectedResult)

        verify(ethDataManager).fetchEthAddress()

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getErc20Balance fails for non-erc20 assets`() {
        subject.getErc20Balance(CryptoCurrency.ETHER)
    }

    @Test
    fun `getErc20Balance delegates to balance cache`() {
        val mockBalance: Erc20Balance = mock()
        val mockResult = mapOf(ERC20_TOKEN to mockBalance)
        whenever(balanceCallCache.getBalances(ACCOUNT_HASH))
            .thenReturn(Single.just(mockResult))

        subject.getErc20Balance(ERC20_TOKEN)
            .test()
            .assertValue(mockBalance)

        verify(balanceCallCache).getBalances(ACCOUNT_HASH)
        verify(ethDataManager).accountAddress

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `getErc20Balance returns zero if asset not found`() {
        val mockBalance: Erc20Balance = mock()
        val mockResult = mapOf(ERC20_TOKEN to mockBalance)
        whenever(balanceCallCache.getBalances(ACCOUNT_HASH))
            .thenReturn(Single.just(mockResult))

        subject.getErc20Balance(UNKNOWN_ERC20_TOKEN)
            .test()
            .assertValue { it.balance.isZero }

        verify(balanceCallCache).getBalances(ACCOUNT_HASH)
        verify(ethDataManager).accountAddress

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getErc20History fails for non-erc20 assets`() {
        subject.getErc20History(CryptoCurrency.ETHER)
    }

    @Test
    fun `getErc20History delegates to history cache`() {
        val mockEvent: Erc20HistoryEvent = mock()
        val mockEventList = listOf(mockEvent)

        whenever(historyCallCache.fetch(ACCOUNT_HASH, ERC20_TOKEN))
            .thenReturn(Single.just(mockEventList))

        subject.getErc20History(ERC20_TOKEN)
            .test()
            .assertValue(mockEventList)

        verify(historyCallCache).fetch(ACCOUNT_HASH, ERC20_TOKEN)
        verify(ethDataManager).accountAddress

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createErc20Transaction fails in called for a non-erc20 token`() {
        subject.getErc20History(CryptoCurrency.ETHER)
    }

    @Test
    fun `createErc20Transaction correctly constructs a transaction`() {
        val nonce = 1001.toBigInteger()
        whenever(ethDataManager.getNonce()).thenReturn(Single.just(nonce))

        val destination = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29"
        val amount = 200.toBigInteger()
        val gasPrice = 5.toBigInteger()
        val gasLimit = 21.toBigInteger()

        val expectedPayload = "a9059cbb0000000000000000000000002ca28ffadd20474ffe2705580279a1e67cd10a29" +
            "00000000000000000000000000000000000000000000000000000000000000c8"

        subject.createErc20Transaction(
            asset = ERC20_TOKEN,
            to = destination,
            amount = amount,
            gasPriceWei = gasPrice,
            gasLimitGwei = gasLimit
        ).test()
            .assertValue { raw ->
                raw.nonce == nonce &&
                raw.gasPrice == gasPrice &&
                raw.gasLimit == gasLimit &&
                raw.to == CONTRACT_ADDRESS &&
                raw.value == BigInteger.ZERO &&
                raw.data == expectedPayload
            }

        verify(ethDataManager).getNonce()

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `signErc20Transaction delegates to eth data manager`() {
        val rawTx: RawTransaction = mock()
        val secondPassword = "SecondPassword"
        val result = "This Is The Signed tx bytes".toByteArray()

        whenever(ethDataManager.signEthTransaction(rawTx, secondPassword))
            .thenReturn(Single.just(result))

        subject.signErc20Transaction(rawTx, secondPassword)
            .test()
            .assertValue { it.contentEquals(result) }

        verify(ethDataManager).signEthTransaction(rawTx, secondPassword)

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `pushErc20Transaction delegates to eth data manager`() {
        val signedBytes = "This Is The Signed tx bytes".toByteArray()
        whenever(ethDataManager.pushEthTx(signedBytes)).thenReturn(Observable.just(TX_HASH))

        subject.pushErc20Transaction(signedBytes)
            .test()
            .assertValue { it == TX_HASH }

        verify(ethDataManager).pushEthTx(signedBytes)

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getErc20TxNote fails in called for a non-erc20 token`() {
        subject.getErc20TxNote(CryptoCurrency.ETHER, TX_HASH)
    }

    @Test
    fun `getErc20TxNote delegates to eth data manager`() {
        val note = "This is a note"
        val notesMap: HashMap<String, String> = mock {
            on { get(TX_HASH) }.thenReturn(note)
        }

        val tokenData: Erc20TokenData = mock {
            on { txNotes }.thenReturn(notesMap)
        }
        whenever(ethDataManager.getErc20TokenData(ERC20_TOKEN)).thenReturn(tokenData)

        val result = subject.getErc20TxNote(ERC20_TOKEN, TX_HASH)

        assertEquals(note, result)

        verify(ethDataManager).getErc20TokenData(ERC20_TOKEN)

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `putErc20TxNote fails in called for a non-erc20 token`() {
        subject.putErc20TxNote(CryptoCurrency.ETHER, TX_HASH, "This is a note")
    }

    @Test
    fun `putErc20TxNote delegates to eth data manager`() {
        val note = "This is a note"

        whenever(
            ethDataManager.updateErc20TransactionNotes(
                ERC20_TOKEN,
                TX_HASH,
                note
            )
        ).thenReturn(Completable.complete())

        subject.putErc20TxNote(ERC20_TOKEN, TX_HASH, note)
            .test()
            .assertComplete()

        verify(ethDataManager).updateErc20TransactionNotes(ERC20_TOKEN, TX_HASH, note)

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `hasUnconfirmedTransactions delegates to eth data manager`() {
        whenever(ethDataManager.isLastTxPending()).thenReturn(Single.just(true))

        subject.hasUnconfirmedTransactions()
            .test()
            .assertValue { it == true }

        verify(ethDataManager).isLastTxPending()

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `latestBlockNumber delegates to eth data manager`() {
        val blockNumber = 19000.toBigInteger()

        val lastBlock: EthLatestBlockNumber = mock {
            on { number }.thenReturn(blockNumber)
        }
        whenever(ethDataManager.getLatestBlockNumber()).thenReturn(Single.just(lastBlock))

        subject.latestBlockNumber()
            .test()
            .assertValue { it == blockNumber }

        verify(ethDataManager).getLatestBlockNumber()

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `isContractAddress delegates for contract address`() {
        whenever(ethDataManager.isContractAddress(CONTRACT_ADDRESS)).thenReturn(Single.just(true))

        subject.isContractAddress(CONTRACT_ADDRESS)
            .test()
            .assertValue(true)

        verify(ethDataManager).isContractAddress(CONTRACT_ADDRESS)

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `isContractAddress delegates for non-contract address`() {
        whenever(ethDataManager.isContractAddress(ACCOUNT_HASH)).thenReturn(Single.just(false))

        subject.isContractAddress(ACCOUNT_HASH)
            .test()
            .assertValue(false)

        verify(ethDataManager).isContractAddress(ACCOUNT_HASH)

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `flushCaches fails in called for a non-erc20 token`() {
        subject.flushCaches(CryptoCurrency.ETHER)
    }

    @Test
    fun `flushCaches clears cached API data`() {
        subject.flushCaches(ERC20_TOKEN)

        verify(balanceCallCache).flush(ERC20_TOKEN)
        verify(historyCallCache).flush(ERC20_TOKEN)

        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(balanceCallCache)
        verifyNoMoreInteractions(historyCallCache)
    }

    companion object {
        const val ACCOUNT_HASH = "0x4058a004dd718babab47e14dd0d744742e5b9903"
        const val CONTRACT_ADDRESS = "0x8e870d67f660d95d5be530380d0ec0bd388289e1"

        const val TX_HASH = "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff"

        private val ERC20_TOKEN: AssetInfo = object : CryptoCurrency(
            ticker = "DUMMY",
            name = "Dummies",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 8,
            l2chain = ETHER,
            l2identifier = CONTRACT_ADDRESS,
            requiredConfirmations = 5,
            colour = "#123456"
        ) { }

        private val UNKNOWN_ERC20_TOKEN: AssetInfo = object : CryptoCurrency(
            ticker = "WHATEVER",
            name = "Whatevs",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 8,
            l2chain = ETHER,
            l2identifier = CONTRACT_ADDRESS,
            requiredConfirmations = 5,
            colour = "#123456"
        ) { }
    }
}
