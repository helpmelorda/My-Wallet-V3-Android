package piuk.blockchain.android.coincore.custodialonly

import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.isCustodialOnly
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.identity.UserIdentity
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

internal class DynamicOnlyTradingAsset(
    override val asset: AssetInfo,
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
    private val addressValidation: String? = null,
    private val availableActions: Set<AssetAction> = emptySet()
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
    override val isCustodialOnly: Boolean = asset.isCustodialOnly
    override val multiWallet: Boolean = false

    override fun initToken(): Completable = Completable.complete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(emptyList())

    override fun loadCustodialAccount(): Single<SingleAccountList> =
        Single.just(
            listOf(
                CustodialTradingAccount(
                    asset = asset,
                    label = labels.getDefaultCustodialWalletLabel(),
                    exchangeRates = exchangeRates,
                    custodialWalletManager = custodialManager,
                    identity = identity,
                    features = features,
                    baseActions = availableActions
                )
            )
        )

    private val addressRegex: Regex? by unsafeLazy {
        addressValidation?.toRegex()
    }

    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> =
        addressRegex?.let {
            if (address.matches(it)) {
                Maybe.just(
                    DynamicCustodialAddress(
                        address = address,
                        asset = asset
                    )
                )
            } else {
                Maybe.empty()
            }
        } ?: Maybe.empty()
}

internal class DynamicCustodialAddress(
    override val address: String,
    override val asset: AssetInfo,
    override val label: String = address
) : CryptoAddress