package piuk.blockchain.androidcore.data.ethereum

import com.blockchain.logging.LastTxUpdater
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.isErc20
import info.blockchain.wallet.ethereum.Erc20TokenData
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.ethereum.data.TransactionState
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.spongycastle.util.encoders.Hex
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import timber.log.Timber
import java.math.BigInteger
import java.util.HashMap

class EthDataManager(
    private val payloadDataManager: PayloadDataManager,
    private val ethAccountApi: EthAccountApi,
    private val ethDataStore: EthDataStore,
    private val metadataManager: MetadataManager,
    private val lastTxUpdater: LastTxUpdater,
    rxBus: RxBus
) {

    private val internalAccountAddress: String?
        get() = ethDataStore.ethWallet?.account?.address

    val accountAddress: String
        get() = internalAccountAddress ?: throw Exception("No ETH address found")

    private val rxPinning = RxPinning(rxBus)

    /**
     * Clears the currently stored ETH account from memory.
     */
    fun clearAccountDetails() {
        ethDataStore.clearData()
    }

    /**
     * Returns an [CombinedEthModel] object for a given ETH address as an [Observable]. An
     * [CombinedEthModel] contains a list of transactions associated with the account, as well
     * as a final balance. Calling this function also caches the [CombinedEthModel].
     *
     * @return An [Observable] wrapping an [CombinedEthModel]
     */
    fun fetchEthAddress(): Observable<CombinedEthModel> =
        rxPinning.call<CombinedEthModel> {
            ethAccountApi.getEthAddress(listOf(accountAddress))
                .map(::CombinedEthModel)
                .doOnNext { ethDataStore.ethAddressResponse = it }
                .subscribeOn(Schedulers.io())
        }

    fun getBalance(account: String): Single<BigInteger> =
        ethAccountApi.getEthAddress(listOf(account))
            .map(::CombinedEthModel)
            .map { it.getTotalBalance() }
            .singleOrError()
            .doOnError(Timber::e)
            .onErrorReturn { BigInteger.ZERO }
            .subscribeOn(Schedulers.io())

    fun updateAccountLabel(label: String): Completable {
        require(label.isNotEmpty())
        check(ethDataStore.ethWallet != null)
        ethDataStore.ethWallet?.renameAccount(label)
        return save()
    }

    /**
     * Returns the user's ETH account object if previously fetched.
     *
     * @return A nullable [CombinedEthModel] object
     */
    fun getEthResponseModel(): CombinedEthModel? = ethDataStore.ethAddressResponse

    /**
     * Returns the user's [EthereumWallet] object if previously fetched.
     *
     * @return A nullable [EthereumWallet] object
     */
    @Deprecated("This shouldn't be directly exposed to higher layers")
    fun getEthWallet(): EthereumWallet? = ethDataStore.ethWallet

    /**
     * Returns a stream of [EthTransaction] objects associated with a user's ETH address specifically
     * for displaying in the transaction list. These are cached and may be empty if the account
     * hasn't previously been fetched.
     *
     * @return An [Observable] stream of [EthTransaction] objects
     */
    fun getEthTransactions(): Single<List<EthTransaction>> =
        internalAccountAddress?.let {
            ethAccountApi.getEthTransactions(listOf(it)).applySchedulers()
        } ?: Single.just(emptyList())

    /**
     * Returns whether or not the user's ETH account currently has unconfirmed transactions, and
     * therefore shouldn't be allowed to send funds until confirmation.
     * We compare the last submitted tx hash with the newly created tx hash - if they match it means
     * that the previous tx has not yet been processed.
     *
     * @return An [Observable] wrapping a [Boolean]
     */
    fun isLastTxPending(): Single<Boolean> =
        internalAccountAddress?.let {
            ethAccountApi.getLastEthTransaction(listOf(it)).map { tx ->
                tx.state.toLocalState() == TransactionState.PENDING
            }.defaultIfEmpty(false)
        } ?: Single.just(false)

    /**
     * Returns a [Number] representing the most recently
     * mined block.
     *
     * @return An [Observable] wrapping a [Number]
     */
    fun getLatestBlockNumber(): Single<EthLatestBlockNumber> =
        ethAccountApi.latestBlockNumber.applySchedulers()

    fun isContractAddress(address: String): Single<Boolean> =
        rxPinning.call<Boolean> {
            ethAccountApi.getIfContract(address)
                .applySchedulers()
        }.singleOrError()

    private fun String.toLocalState() =
        when (this) {
            "PENDING" -> TransactionState.PENDING
            "CONFIRMED" -> TransactionState.CONFIRMED
            "REPLACED" -> TransactionState.REPLACED
            else -> TransactionState.UNKNOWN
        }

    /**
     * Returns the transaction notes for a given transaction hash, or null if not found.
     */
    fun getTransactionNotes(hash: String): String? = ethDataStore.ethWallet?.txNotes?.get(hash)

    /**
     * Puts a given note in the [HashMap] of transaction notes keyed to a transaction hash. This
     * information is then saved in the metadata service.
     *
     * @return A [Completable] object
     */
    fun updateTransactionNotes(hash: String, note: String): Completable =
        ethDataStore.ethWallet?.let {
            it.txNotes[hash] = note
            return@let save()
        } ?: Completable.error { IllegalStateException("ETH Wallet is null") }
            .applySchedulers()

    internal fun updateErc20TransactionNotes(
        asset: AssetInfo,
        hash: String,
        note: String
    ): Completable {
        require(asset.isErc20())

        return rxPinning.call {
            getErc20TokenData(asset).putTxNote(hash, note)
            return@call save()
        }.applySchedulers()
    }

    /**
     * Fetches EthereumWallet stored in metadata. If metadata entry doesn't exists it will be created.
     *
     * @param label The default address default to be used if metadata entry doesn't exist
     * @return An [Completable]
     */
    fun initEthereumWallet(
        assetCatalogue: AssetCatalogue,
        label: String
    ): Completable =
        fetchOrCreateEthereumWallet(assetCatalogue, label)
            .flatMapCompletable { (wallet, needsSave) ->
                ethDataStore.ethWallet = wallet
                if (needsSave) {
                    save()
                } else {
                    Completable.complete()
                }
            }

    /**
     * @param gasPriceWei Represents the fee the sender is willing to pay for gas. One unit of gas
     *                 corresponds to the execution of one atomic instruction, i.e. a computational step
     * @param gasLimitGwei Represents the maximum number of computational steps the transaction
     *                 execution is allowed to take
     * @param weiValue The amount of wei to transfer from the sender to the recipient
     */
    fun createEthTransaction(
        nonce: BigInteger,
        to: String,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        weiValue: BigInteger
    ): RawTransaction? = RawTransaction.createEtherTransaction(
        nonce,
        gasPriceWei,
        gasLimitGwei,
        to,
        weiValue
    )

    fun getTransaction(hash: String): Observable<EthTransaction> =
        rxPinning.call<EthTransaction> {
            ethAccountApi.getTransaction(hash)
                .applySchedulers()
        }

    fun getNonce(): Single<BigInteger> =
        fetchEthAddress()
            .singleOrError()
            .map {
                it.getNonce()
            }

    fun signEthTransaction(rawTransaction: RawTransaction, secondPassword: String = ""): Single<ByteArray> =
        Single.fromCallable {
            if (payloadDataManager.isDoubleEncrypted) {
                payloadDataManager.decryptHDWallet(secondPassword)
            }
            val account = ethDataStore.ethWallet?.account ?: throw IllegalStateException("No Eth wallet defined")
            account.signTransaction(rawTransaction, payloadDataManager.masterKey)
        }

    fun pushEthTx(signedTxBytes: ByteArray): Observable<String> =
        rxPinning.call<String> {
            ethAccountApi.pushTx("0x" + String(Hex.encode(signedTxBytes)))
                .flatMap {
                    lastTxUpdater.updateLastTxTime()
                        .onErrorComplete()
                        .andThen(Observable.just(it))
                }
                .applySchedulers()
        }

    fun pushTx(signedTxBytes: ByteArray): Single<String> =
        pushEthTx(signedTxBytes).singleOrError()

    private fun fetchOrCreateEthereumWallet(
        assetCatalogue: AssetCatalogue,
        label: String
    ): Single<Pair<EthereumWallet, Boolean>> =
        metadataManager.fetchMetadata(EthereumWallet.METADATA_TYPE_EXTERNAL).defaultIfEmpty("")
            .map { metadata ->
                val walletJson = if (metadata != "") metadata else null

                var ethWallet = EthereumWallet.load(walletJson)
                var needsSave = false

                if (ethWallet?.account == null || !ethWallet.account.isCorrect) {
                    try {
                        val masterKey = payloadDataManager.masterKey
                        ethWallet = EthereumWallet(masterKey, label)
                        needsSave = true
                    } catch (e: HDWalletException) {
                        // Wallet private key unavailable. First decrypt with second password.
                        throw InvalidCredentialsException(e.message)
                    }
                }

                if (ethWallet.updateErc20Tokens(assetCatalogue, label)) {
                    needsSave = true
                }

                if (!ethWallet.account.isAddressChecksummed()) {
                    ethWallet.account.apply {
                        address = withChecksummedAddress()
                    }
                    needsSave = true
                }
                ethWallet to needsSave
            }

    fun save(): Completable =
        metadataManager.saveToMetadata(
            ethDataStore.ethWallet!!.toJson(),
            EthereumWallet.METADATA_TYPE_EXTERNAL
        )

    fun getErc20TokenData(asset: AssetInfo): Erc20TokenData {
        require(asset.l2chain == CryptoCurrency.ETHER)
        require(asset.l2identifier != null)
        val name = asset.ticker.lowercase()

        return getEthWallet()!!.getErc20TokenData(name)
    }

    val requireSecondPassword: Boolean
        get() = payloadDataManager.isDoubleEncrypted
}
