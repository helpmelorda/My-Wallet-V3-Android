package piuk.blockchain.android.coincore.erc20

import com.blockchain.annotations.CommonCode
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.isCustodialOnly
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.identity.UserIdentity
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class Erc20Asset(
    override val asset: AssetInfo,
    private val erc20DataManager: Erc20DataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatus,
    payloadManager: PayloadDataManager,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ExchangeRateService,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    identity: UserIdentity,
    features: InternalFeatureFlagApi,
    private val availableCustodialActions: Set<AssetAction>,
    private val availableNonCustodialActions: Set<AssetAction>
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger,
    identity,
    features
) {
    private val erc20address
        get() = erc20DataManager.accountHash

    override val isCustodialOnly: Boolean = asset.isCustodialOnly
    override val multiWallet: Boolean = false

    override fun initToken(): Completable = Completable.complete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(getNonCustodialAccount())
            .map { listOf(it) }

    override fun loadCustodialAccounts(): Single<SingleAccountList> =
        Single.just(
            listOf(
                CustodialTradingAccount(
                    asset = asset,
                    label = labels.getDefaultCustodialWalletLabel(),
                    exchangeRates = exchangeRates,
                    custodialWalletManager = custodialManager,
                    identity = identity,
                    features = features,
                    baseActions = availableCustodialActions
                )
            )
        )

    private fun getNonCustodialAccount(): Erc20NonCustodialAccount =
        Erc20NonCustodialAccount(
            payloadManager,
            asset,
            erc20DataManager,
            erc20address,
            feeDataManager,
            labels.getDefaultNonCustodialWalletLabel(),
            exchangeRates,
            walletPreferences,
            custodialManager,
            availableNonCustodialActions,
            identity
        )

    @CommonCode("Exists in EthAsset")
    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> =
        Single.just(isValidAddress(address)).flatMapMaybe { isValid ->
            if (isValid) {
                erc20DataManager.isContractAddress(address)
                    .flatMapMaybe { isContract ->
                        if (isContract) {
                            throw AddressParseError(AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
                        } else {
                            Maybe.just(Erc20Address(asset, address, label ?: address))
                        }
                    }
            } else {
                Maybe.empty()
            }
        }

    override fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)
}
