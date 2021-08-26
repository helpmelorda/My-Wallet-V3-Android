package piuk.blockchain.android.coincore.loader

import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.isCustodial
import info.blockchain.balance.isCustodialOnly
import info.blockchain.balance.isErc20
import info.blockchain.balance.isNonCustodial
import io.reactivex.rxjava3.core.Completable
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.coincore.custodialonly.DynamicOnlyTradingAsset
import piuk.blockchain.android.coincore.erc20.Erc20Asset
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.CoincoreInitFailure
import piuk.blockchain.android.coincore.NonCustodialSupport
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import java.lang.IllegalStateException

// This is a rubbish regex, but it'll do until I'm provided a better one
private const val defaultCustodialAddressValidation = "[a-zA-Z0-9]{15,}"

internal class DynamicAssetLoader(
    private val nonCustodialAssets: List<CryptoAsset>,
    private val assetCatalogue: AssetCatalogueImpl,
    private val featureConfig: AssetRemoteFeatureLookup,
    private val payloadManager: PayloadDataManager,
    private val erc20DataManager: Erc20DataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatus,
    private val custodialManager: CustodialWalletManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val tradingBalances: TradingBalanceDataManager,
    private val interestBalances: InterestBalanceDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val labels: DefaultLabels,
    private val pitLinking: PitLinking,
    private val crashLogger: CrashLogger,
    private val identity: UserIdentity,
    private val features: InternalFeatureFlagApi
) : AssetLoader {

    private val assetMap = mutableMapOf<AssetInfo, CryptoAsset>()

    override operator fun get(asset: AssetInfo): CryptoAsset =
        assetMap[asset] ?: attemptLoadAsset(asset)

    private fun attemptLoadAsset(assetInfo: AssetInfo): CryptoAsset =
        when {
            assetInfo.isErc20() -> loadErc20Asset(assetInfo)
            assetInfo.isCustodialOnly -> loadCustodialOnlyAsset(assetInfo)
            else -> throw IllegalStateException("Unknown asset type enabled: ${assetInfo.ticker}")
        }.also {
            check(!assetMap.containsKey(assetInfo)) { "Asset already loaded" }
            assetMap[assetInfo] = it
        }

    override fun initAndPreload(): Completable =
        assetCatalogue.initialise(nonCustodialAssets.map { it.asset }.toSet())
            .doOnSubscribe { crashLogger.logEvent("Coincore init started") }
            .flatMap { supportedAssets ->
                // We need to make sure than any l1 assets - notably ETH - is initialised before
                // create any l2s. So that things like balance calls will work
                initNonCustodialAssets(nonCustodialAssets)
                    .thenSingle { doLoadAssets(supportedAssets) }
            }
            .map { nonCustodialAssets + it }
            .doOnSuccess { assetList -> assetMap.putAll(assetList.associateBy { it.asset }) }
            .ignoreElement()

    private fun initNonCustodialAssets(assetList: List<CryptoAsset>): Completable =
        Completable.concat(
            assetList.filterIsInstance<NonCustodialSupport>()
                .map { asset ->
                    Completable.defer { asset.initToken() }
                        .doOnError {
                            crashLogger.logException(
                                CoincoreInitFailure("Failed init: ${(asset as CryptoAsset).asset.ticker}", it)
                        )
                    }
            }.toList()
        )

    private fun doLoadAssets(
        dynamicAssets: Set<AssetInfo>
    ): Single<List<CryptoAsset>> {
        val erc20assets = dynamicAssets.filter { it.isErc20() }
        val custodialAssets = dynamicAssets.filter { it.isCustodial && !erc20assets.contains(it) }

        // Those two sets should NOT overlap
        check(erc20assets.intersect(custodialAssets).isEmpty())

        return Single.zip(
            loadErc20Assets(erc20assets),
            loadCustodialOnlyAssets(custodialAssets)
        ) { erc20List, custodialList ->
            erc20List + custodialList
        }
    }

    private fun loadCustodialOnlyAssets(
        custodialAssets: Iterable<AssetInfo>
    ): Single<List<CryptoAsset>> =
        Single.zip(
            tradingBalances.getActiveAssets(),
            interestBalances.getActiveAssets()
        ) { activeTrading, activeInterest ->
            activeInterest + activeTrading
        }.map { activeAssets ->
            custodialAssets.filter { activeAssets.contains(it) }
        }.map { activeSupportedAssets ->
            activeSupportedAssets.map { asset ->
                loadCustodialOnlyAsset(asset)
            }
        }

    private fun loadErc20Assets(
        erc20Assets: Iterable<AssetInfo>
    ): Single<List<CryptoAsset>> =
        Single.zip(
            tradingBalances.getActiveAssets(),
            interestBalances.getActiveAssets(),
            erc20DataManager.getActiveAssets()
        ) { activeTrading, activeInterest, activeNoncustodial ->
            activeInterest + activeTrading + activeNoncustodial
        }.map { activeAssets ->
            erc20Assets.filter { activeAssets.contains(it) }
        }.map { activeSupportedAssets ->
            activeSupportedAssets.map { asset ->
                loadErc20Asset(asset)
            }
        }

    private fun loadCustodialOnlyAsset(assetInfo: AssetInfo): CryptoAsset {
        val possibleActions = featureConfig.featuresFor(assetInfo).toCustodialActions()

        return DynamicOnlyTradingAsset(
            asset = assetInfo,
            payloadManager = payloadManager,
            custodialManager = custodialManager,
            tradingBalances = tradingBalances,
            interestBalances = interestBalances,
            exchangeRates = exchangeRates,
            currencyPrefs = currencyPrefs,
            labels = labels,
            pitLinking = pitLinking,
            crashLogger = crashLogger,
            identity = identity,
            features = features,
            addressValidation = defaultCustodialAddressValidation,
            availableActions = possibleActions
        )
    }

    private fun loadErc20Asset(assetInfo: AssetInfo): CryptoAsset {
        require(assetInfo.l2chain == CryptoCurrency.ETHER)
        require(assetInfo.isCustodial)
        require(assetInfo.isNonCustodial)

        val featureSet = featureConfig.featuresFor(assetInfo)

        return Erc20Asset(
            asset = assetInfo,
            payloadManager = payloadManager,
            erc20DataManager = erc20DataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            currencyPrefs = currencyPrefs,
            custodialManager = custodialManager,
            tradingBalances = tradingBalances,
            interestBalances = interestBalances,
            crashLogger = crashLogger,
            labels = labels,
            pitLinking = pitLinking,
            walletPreferences = walletPreferences,
            identity = identity,
            features = features,
            availableCustodialActions = featureSet.toCustodialActions(),
            availableNonCustodialActions = featureSet.toNonCustodialActions()
        )
    }

    override val loadedAssets: List<CryptoAsset>
        get() = assetMap.values.toList()
}

private fun Set<RemoteFeature>.toCustodialActions() =
    this.mapNotNull {
        when (it) {
            RemoteFeature.CanBuy -> AssetAction.Buy
            RemoteFeature.CanSell -> AssetAction.Sell
            RemoteFeature.CanSwap -> AssetAction.Swap
            RemoteFeature.CanSend -> AssetAction.Send
            RemoteFeature.CanReceive -> AssetAction.Receive
            else -> null
        }
    }.toSet() + setOf(
        AssetAction.ViewActivity, // Can always view activity
        AssetAction.InterestDeposit // Interest is managed in the asset class
    )

private fun Set<RemoteFeature>.toNonCustodialActions() =
    this.mapNotNull {
        when (it) {
            RemoteFeature.CanBuy -> AssetAction.Buy
            RemoteFeature.CanSell -> AssetAction.Sell
            RemoteFeature.CanSwap -> AssetAction.Swap
            else -> null
        }
    }.toSet() + setOf(
        AssetAction.ViewActivity, // Can always view activity
        AssetAction.InterestDeposit, // Interest is managed in the asset class
        AssetAction.Send, // Non-custodial can always send and receive
        AssetAction.Receive
    )
