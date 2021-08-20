package piuk.blockchain.android.coincore

import com.blockchain.logging.CrashLogger
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import piuk.blockchain.android.coincore.impl.TxProcessorFactory
import piuk.blockchain.android.coincore.loader.AssetCatalogueImpl
import piuk.blockchain.android.coincore.loader.AssetLoader
import piuk.blockchain.android.ui.sell.ExchangePriceWithDelta
import piuk.blockchain.android.ui.transfer.AccountsSorter
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import timber.log.Timber

private class CoincoreInitFailure(msg: String, e: Throwable) : Exception(msg, e)

class Coincore internal constructor(
    private val fixedAssets: List<CryptoAsset>,
    private val assetCatalogue: AssetCatalogueImpl,
    private val assetLoader: AssetLoader,
    // TODO: Build an interface on PayloadDataManager/PayloadManager for 'global' crypto calls; second password etc?
    private val payloadManager: PayloadDataManager,
    private val txProcessorFactory: TxProcessorFactory,
    private val defaultLabels: DefaultLabels,
    private val fiatAsset: Asset,
    private val crashLogger: CrashLogger
) {

    private lateinit var assetMap: Map<AssetInfo, CryptoAsset>

    operator fun get(ccy: AssetInfo): CryptoAsset =
        assetMap[ccy] ?: throw IllegalArgumentException(
            "Unknown CryptoCurrency ${ccy.ticker}"
        )

    fun init(): Completable =
        assetCatalogue.initialise(fixedAssets.map { it.asset }.toSet())
            .doOnSubscribe { crashLogger.logEvent("Coincore init start") }
            .flatMap { assetLoader.loadDynamicAssets(it) }
            .map { fixedAssets + it }
            .doOnSuccess { assetList -> assetMap = assetList.associateBy { it.asset } }
            .flatMapCompletable { assetList -> initAssets(assetList) }
            .doOnComplete {
                crashLogger.logEvent("Coincore init complete")
            }
            .doOnError {
                val msg = "Coincore initialisation failed! $it"
                crashLogger.logEvent(msg)
                Timber.e(msg)
            }

    private fun initAssets(assetList: List<CryptoAsset>): Completable =
        Completable.concat(
            assetList.map { asset ->
                Completable.defer { asset.init() }
                    .doOnError {
                        crashLogger.logException(
                            CoincoreInitFailure("Failed init: ${asset.asset.ticker}", it)
                        )
                    }
            }.toList()
        )

    val fiatAssets: Asset
        get() = fiatAsset

    val cryptoAssets: Iterable<CryptoAsset>
        get() = assetMap.values.filter { it.isEnabled }

    val allAssets: Iterable<Asset>
        get() = listOf(fiatAsset) + cryptoAssets

    fun validateSecondPassword(secondPassword: String) =
        payloadManager.validateSecondPassword(secondPassword)

    fun allWallets(includeArchived: Boolean = false): Single<AccountGroup> =
        Maybe.concat(
            allAssets.map {
                it.accountGroup().map { grp -> grp.accounts }
                    .map { list ->
                        list.filter { account ->
                            (includeArchived || account !is CryptoAccount) || !account.isArchived
                        }
                    }
            }
        ).reduce { a, l -> a + l }
            .map { list ->
                AllWalletsAccount(list, defaultLabels) as AccountGroup
            }.toSingle()

    fun allWalletsWithActions(
        actions: Set<AssetAction>,
        sorter: AccountsSorter = { Single.just(it) }
    ): Single<SingleAccountList> =
        allWallets()
            .flattenAsObservable { it.accounts }
            .flatMapMaybe { account ->
                account.actions.flatMapMaybe { availableActions ->
                    if (availableActions.containsAll(actions)) Maybe.just(account) else Maybe.empty()
                }
            }
            .toList()
            .flatMap { list ->
                sorter(list)
            }

    fun getTransactionTargets(
        sourceAccount: CryptoAccount,
        action: AssetAction
    ): Single<SingleAccountList> {
        val sameCurrencyTransactionTargets =
            get(sourceAccount.asset).transactionTargets(sourceAccount)

        val fiatTargets = fiatAsset.accountGroup(AssetFilter.All).map {
            it.accounts
        }.defaultIfEmpty(emptyList())

        val sameCurrencyPlusFiat = sameCurrencyTransactionTargets.zipWith(fiatTargets) { crypto, fiat ->
            crypto + fiat
        }

        return allWallets().map { it.accounts }.flatMap { allWallets ->
            if (action != AssetAction.Swap) {
                sameCurrencyPlusFiat
            } else
                Single.just(allWallets)
        }.map {
            it.filter(getActionFilter(action, sourceAccount))
        }
    }

    private fun getActionFilter(
        action: AssetAction,
        sourceAccount: CryptoAccount
    ): (SingleAccount) -> Boolean =
        when (action) {
            AssetAction.Sell -> {
                {
                    it is FiatAccount
                }
            }

            AssetAction.Swap -> {
                {
                    it is CryptoAccount &&
                        it.asset != sourceAccount.asset &&
                        it !is FiatAccount &&
                        it !is InterestAccount &&
                        if (sourceAccount.isCustodial()) it.isCustodial() else true
                }
            }
            AssetAction.Send -> {
                {
                    it !is FiatAccount
                }
            }
            AssetAction.InterestWithdraw -> {
                {
                    it is CryptoAccount && it.asset == sourceAccount.asset
                }
            }
            else -> {
                { true }
            }
        }

    fun findAccountByAddress(
        asset: AssetInfo,
        address: String
    ): Maybe<SingleAccount> =
        filterAccountsByAddress(assetMap.getValue(asset).accountGroup(AssetFilter.All), address)

    private fun filterAccountsByAddress(
        accountGroup: Maybe<AccountGroup>,
        address: String
    ): Maybe<SingleAccount> =
        accountGroup.map {
            it.accounts
        }.flattenAsObservable { it }
            .flatMapSingle { account ->
                account.receiveAddress
                    .map { it as CryptoAddress }
                    .onErrorReturn { NullCryptoAddress }
                    .map { cryptoAccount ->
                        when {
                            cryptoAccount.address.equals(address, true) -> account
                            account.doesAddressBelongToWallet(address) -> account
                            else -> NullCryptoAccount()
                        }
                    }
            }.filter { it != NullCryptoAccount() }
            .toList()
            .flatMapMaybe {
                if (it.isEmpty())
                    Maybe.empty()
                else
                    Maybe.just(it.first())
            }

    fun createTransactionProcessor(
        source: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        txProcessorFactory.createProcessor(
            source,
            target,
            action
        )

    fun getExchangePriceWithDelta(asset: AssetInfo): Single<ExchangePriceWithDelta> =
        this[asset].exchangeRate().zipWith(
            this[asset].getPricesWith24hDelta()
        ) { currentPrice, priceDelta ->
            ExchangePriceWithDelta(currentPrice.price(), priceDelta.delta24h)
        }

    @Suppress("SameParameterValue")
    private fun allAccounts(includeArchived: Boolean = false): Observable<SingleAccount> =
        allWallets(includeArchived).map { it.accounts }
            .flattenAsObservable { it }

    fun isLabelUnique(label: String): Single<Boolean> =
        allAccounts(true)
            .filter { a -> a.label.compareTo(label, true) == 0 }
            .toList()
            .map { it.isEmpty() }

    // "Active" means funded, but this is not yet supported until erc20 scaling is complete TODO
    fun activeCryptoAssets(): List<AssetInfo> = assetCatalogue.supportedCryptoAssets

    fun supportedFiatAssets(): List<String> = assetCatalogue.supportedFiatAssets
}
