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
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.coincore.custodialonly.DynamicOnlyTradingAsset
import piuk.blockchain.android.coincore.erc20.Erc20Asset
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import java.lang.IllegalStateException
import io.reactivex.rxjava3.core.Single

// TODO this will change to support both fiat and crypto, when we have a common interface/class for both
interface AssetLoader {
    fun loadDynamicAssets(dynamicAssets: Set<AssetInfo>): Single<List<CryptoAsset>>
}

// This is a rubbish regex, but it'll do until I'm provided a better one
private const val defaultCustodialAddressValidation = "[a-zA-Z0-9]{15,}"

internal class CryptoAssetLoader(
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

    // When building the asset list, make sure that any l1 asset are earlier in the list than any l2 for that
    // chain -  ETH before ERC20 - to ensure that asset init works correctly.
    // Since, ETH is a fixed asset and erc20 are appended, we don't need to do anything special at this time
    override fun loadDynamicAssets(
        dynamicAssets: Set<AssetInfo>
    ): Single<List<CryptoAsset>> =
        Single.fromCallable { doLoadAssets(dynamicAssets) }

    private fun doLoadAssets(
        dynamicAssets: Set<AssetInfo>
    ): List<CryptoAsset> =
        dynamicAssets.map {
            when {
                it.isErc20() -> loadErc20Asset(it)
                it.isCustodialOnly -> loadCustodialOnlyAsset(it)
                else -> throw IllegalStateException("Unknown asset type enabled: ${it.ticker}")
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
