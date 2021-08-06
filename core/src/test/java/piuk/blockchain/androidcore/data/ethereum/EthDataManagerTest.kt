package piuk.blockchain.androidcore.data.ethereum

import com.blockchain.android.testutils.rxInit
import com.blockchain.logging.LastTxUpdater
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.keys.MasterKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.amshove.kluent.`should contain`
import org.amshove.kluent.`should be equal to`
import com.nhaarman.mockitokotlin2.any

import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import java.math.BigInteger

class EthDataManagerTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    private val payloadManager: PayloadDataManager = mock()
    private val ethAccountApi: EthAccountApi = mock()
    private val ethDataStore: EthDataStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val metadataManager: MetadataManager = mock()
    private val lastTxUpdater: LastTxUpdater = mock()
    private val rxBus = RxBus()

    private val subject = EthDataManager(
        payloadDataManager = payloadManager,
        ethAccountApi = ethAccountApi,
        ethDataStore = ethDataStore,
        metadataManager = metadataManager,
        lastTxUpdater = lastTxUpdater,
        rxBus = rxBus
    )

    @Test
    fun clearEthAccountDetails() {
        // Arrange

        // Act
        subject.clearAccountDetails()

        // Assert
        verify(ethDataStore).clearData()
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun fetchEthAddress() {
        // Arrange
        val ethAddress = "ADDRESS"
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn(ethAddress)
        val ethAddressResponseMap: EthAddressResponseMap = mock()
        whenever(ethAccountApi.getEthAddress(listOf(ethAddress)))
            .thenReturn(Observable.just(ethAddressResponseMap))
        // Act
        val testObserver = subject.fetchEthAddress().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(ethDataStore, atLeastOnce()).ethWallet
        verify(ethDataStore).ethAddressResponse = any()
        verifyZeroInteractions(ethDataStore)
        verify(ethAccountApi).getEthAddress(listOf(ethAddress))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun `get balance found`() {
        // Arrange
        val ethAddress = "ADDRESS"
        val ethAddressResponseMap: EthAddressResponseMap = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val response: EthAddressResponse = mock()
        whenever(response.balance).thenReturn(BigInteger.TEN)
        whenever(ethAddressResponseMap.ethAddressResponseMap.values).thenReturn(mutableListOf(response))
        whenever(ethAccountApi.getEthAddress(listOf(ethAddress)))
            .thenReturn(Observable.just(ethAddressResponseMap))
        // Act
        val testObserver = subject.getBalance(ethAddress).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(BigInteger.TEN)
        verify(ethAccountApi).getEthAddress(listOf(ethAddress))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun `get balance error, still returns value`() {
        // Arrange
        val ethAddress = "ADDRESS"
        whenever(ethAccountApi.getEthAddress(listOf(ethAddress)))
            .thenReturn(Observable.error(Exception()))
        // Act
        val testObserver = subject.getBalance(ethAddress).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(BigInteger.ZERO)
        verify(ethAccountApi).getEthAddress(listOf(ethAddress))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun getEthResponseModel() {
        // Arrange

        // Act
        subject.getEthResponseModel()
        // Assert
        verify(ethDataStore).ethAddressResponse
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun getEthWallet() {
        // Arrange
        // Act
        subject.getEthWallet()
        // Assert
        verify(ethDataStore).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun `getEthTransactions response found with 3 transactions`() {
        // Arrange
        val ethAddress = "ADDRESS"
        val ethTransaction: EthTransaction = mock()
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn(ethAddress)
        whenever(ethAccountApi.getEthTransactions(any()))
            .thenReturn(Single.just(listOf(ethTransaction, ethTransaction, ethTransaction)))
        // Act
        val testObserver = subject.getEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val values = testObserver.values()
        values[0] `should contain` ethTransaction

        values.size `should be equal to` 1
    }

    @Test
    fun `getEthTransactions response not found`() {
        // Arrange
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn(null)
        // Act
        val testObserver = subject.getEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, emptyList())
    }

    @Test
    fun `lastTx is pending when there is at least one transaction pending`() {
        // Arrange
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn("Address")

        whenever(ethAccountApi.getLastEthTransaction(any()))
            .thenReturn(Maybe.just(EthTransaction(state = "PENDING")))
        // Act
        val result = subject.isLastTxPending().test()
        // Assert

        result.assertValueCount(1)
        result.assertValueAt(0, true)
    }

    @Test
    fun `lastTx is not pending when there is no pending tx`() {
        // Arrange
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn("Address")

        whenever(ethAccountApi.getLastEthTransaction(any()))
            .thenReturn(Maybe.just(EthTransaction(state = "CONFIRMED")))
        // Act
        val result = subject.isLastTxPending().test()
        // Assert

        result.assertValueCount(1)
        result.assertValueAt(0, false)
    }

    @Test
    fun `lastTx is not pending when there is no tx`() {
        // Arrange
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn("Address")

        whenever(ethAccountApi.getLastEthTransaction(any()))
            .thenReturn(Maybe.empty())
        // Act
        val result = subject.isLastTxPending().test()
        // Assert

        result.assertValueCount(1)
        result.assertValueAt(0, false)
    }

    @Test
    fun getLatestBlock() {
        // Arrange
        val latestBlock = EthLatestBlockNumber()
        whenever(ethAccountApi.latestBlockNumber).thenReturn(Single.just(latestBlock))
        // Act
        val testObserver = subject.getLatestBlockNumber().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(latestBlock)
        verify(ethAccountApi).latestBlockNumber
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun getIfContract() {
        // Arrange
        val address = "ADDRESS"
        whenever(ethAccountApi.getIfContract(address)).thenReturn(Observable.just(true))
        // Act
        val testObserver = subject.isContractAddress(address).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
        verify(ethAccountApi).getIfContract(address)
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun `getTransactionNotes returns string object`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        whenever(ethDataStore.ethWallet!!.txNotes[hash]).thenReturn(notes)
        // Act
        val result = subject.getTransactionNotes(hash)
        // Assert
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        result `should be equal to` notes
    }

    @Test
    fun `getTransactionNotes returns null object as wallet is missing`() {
        // Arrange
        val hash = "HASH"
        whenever(ethDataStore.ethWallet).thenReturn(null)
        // Act
        val result = subject.getTransactionNotes(hash)
        // Assert
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        result `should be equal to` null
    }

    @Test
    fun `updateTransactionNotes success`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        val ethereumWallet: EthereumWallet = mock()
        whenever(ethDataStore.ethWallet).thenReturn(ethereumWallet)
        whenever(ethDataStore.ethWallet!!.toJson()).thenReturn("{}")
        whenever(metadataManager.saveToMetadata(any(), any())).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateTransactionNotes(hash, notes).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        verify(metadataManager).saveToMetadata(any(), any())
        verifyNoMoreInteractions(metadataManager)
    }

    @Test
    fun `updateTransactionNotes wallet not found`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        whenever(ethDataStore.ethWallet).thenReturn(null)
        // Act
        val testObserver = subject.updateTransactionNotes(hash, notes).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(IllegalStateException::class.java)
        verify(ethDataStore).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun signEthTransaction() {
        // Arrange
        val rawTransaction: RawTransaction = mock()
        val byteArray = ByteArray(32)
        val masterKey: MasterKey = mock()

        whenever(ethDataStore.ethWallet!!.account!!.signTransaction(
            eq(rawTransaction),
            eq(masterKey)
        )).thenReturn(byteArray)

        whenever(payloadManager.masterKey).thenReturn(masterKey)

        // Act
        val testObserver = subject.signEthTransaction(rawTransaction, "").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(byteArray)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun pushEthTx() {
        // Arrange
        val byteArray = ByteArray(32)
        val hash = "HASH"
        whenever(ethAccountApi.pushTx(any())).thenReturn(Observable.just(hash))
        whenever(lastTxUpdater.updateLastTxTime()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.pushEthTx(byteArray).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(hash)
        verify(ethAccountApi).pushTx(any())
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun `pushEthTx returns hash despite update last tx failing`() {
        // Arrange
        val byteArray = ByteArray(32)
        val hash = "HASH"
        whenever(ethAccountApi.pushTx(any())).thenReturn(Observable.just(hash))
        whenever(lastTxUpdater.updateLastTxTime()).thenReturn(Completable.error(Exception()))

        // Act
        val testObserver = subject.pushEthTx(byteArray).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(hash)
        verify(ethAccountApi).pushTx(any())
        verifyNoMoreInteractions(ethAccountApi)
    }
}
