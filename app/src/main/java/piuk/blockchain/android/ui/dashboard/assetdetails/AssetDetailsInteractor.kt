package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.FundsAccount
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyPaymentDetails
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.remoteconfig.FeatureFlag
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAsset

typealias AssetDisplayMap = Map<AssetFilter, AssetDisplayInfo>

sealed class AssetDetailsItem {
    data class CryptoDetailsInfo(
        val assetFilter: AssetFilter,
        val account: BlockchainAccount,
        val balance: Money,
        val fiatBalance: Money,
        val actions: Set<AssetAction>,
        val interestRate: Double = Double.NaN
    ) : AssetDetailsItem()

    data class RecurringBuyInfo(
        val recurringBuy: RecurringBuy
    ) : AssetDetailsItem()

    object RecurringBuyBanner : AssetDetailsItem()
}

data class AssetDisplayInfo(
    val account: BlockchainAccount,
    val amount: Money,
    val pendingAmount: Money,
    val fiatValue: Money,
    val actions: Set<AssetAction>,
    val interestRate: Double = Double.NaN
)

class AssetDetailsInteractor(
    private val interestFeatureFlag: FeatureFlag,
    private val dashboardPrefs: DashboardPrefs,
    private val coincore: Coincore,
    private val custodialWalletManager: CustodialWalletManager
) {

    fun loadAssetDetails(asset: CryptoAsset) =
        getAssetDisplayDetails(asset)

    fun loadExchangeRate(asset: CryptoAsset): Single<String> =
        asset.exchangeRate().map {
            it.price().toStringWithSymbol()
        }

    fun loadHistoricPrices(asset: CryptoAsset, timeSpan: HistoricalTimeSpan): Single<HistoricalRateList> =
        asset.historicRateSeries(timeSpan)
            .onErrorResumeNext { Single.just(emptyList()) }

    fun shouldShowCustody(asset: AssetInfo): Single<Boolean> {
        return coincore[asset].accountGroup(AssetFilter.Custodial)
            .flatMapSingle { it.accountBalance }
            .map {
                !dashboardPrefs.isCustodialIntroSeen && !it.isZero
            }.defaultIfEmpty(false)
    }

    fun loadPaymentDetails(
        paymentMethodType: PaymentMethodType,
        paymentMethodId: String,
        originCurrency: String
    ): Single<RecurringBuyPaymentDetails> {
        return when (paymentMethodType) {
            PaymentMethodType.PAYMENT_CARD -> custodialWalletManager.getCardDetails(paymentMethodId)
                .map { it }
            PaymentMethodType.BANK_TRANSFER -> custodialWalletManager.getLinkedBank(paymentMethodId)
                .map { it.toPaymentMethod() }
            PaymentMethodType.FUNDS -> Single.just(FundsAccount(currency = originCurrency))

            else -> Single.just(object : RecurringBuyPaymentDetails {
                override val paymentDetails: PaymentMethodType
                    get() = paymentMethodType
            })
        }
    }

    fun deleteRecurringBuy(id: String) = custodialWalletManager.cancelRecurringBuy(id)

    private sealed class Details {
        object NoDetails : Details()
        class DetailsItem(
            val isEnabled: Boolean,
            val account: BlockchainAccount,
            val balance: Money,
            val pendingBalance: Money,
            val actions: AvailableActions
        ) : Details()
    }

    private fun Maybe<AccountGroup>.mapDetails(): Single<Details> =
        this.flatMap { grp ->
            Single.zip(
                grp.accountBalance,
                grp.pendingBalance,
                grp.isEnabled,
                grp.actions
            ) { accBalance, pendingBalance, enable, actions ->
                Details.DetailsItem(
                    isEnabled = enable,
                    account = grp,
                    balance = accBalance,
                    pendingBalance = pendingBalance,
                    actions = actions
                ) as Details
            }.toMaybe()
        }.defaultIfEmpty(Details.NoDetails)

    private fun getAssetDisplayDetails(asset: CryptoAsset): Single<AssetDisplayMap> {
        return Singles.zip(
            asset.exchangeRate(),
            asset.accountGroup(AssetFilter.NonCustodial).mapDetails(),
            asset.accountGroup(AssetFilter.Custodial).mapDetails(),
            asset.accountGroup(AssetFilter.Interest).mapDetails(),
            asset.interestRate(),
            interestFeatureFlag.enabled
        ) { fiatRate, nonCustodial, custodial, interest, interestRate, interestEnabled ->
            makeAssetDisplayMap(
                fiatRate, nonCustodial, custodial, interest, interestRate, interestEnabled
            )
        }
    }

    private fun makeAssetDisplayMap(
        fiatRate: ExchangeRate,
        nonCustodial: Details,
        custodial: Details,
        interest: Details,
        interestRate: Double,
        interestEnabled: Boolean
    ): AssetDisplayMap = mutableMapOf<AssetFilter, AssetDisplayInfo>().apply {
        if (nonCustodial !is Details.NoDetails) {
            addToDisplayMap(this, AssetFilter.NonCustodial, nonCustodial, fiatRate)
        }

        if (custodial !is Details.NoDetails) {
            addToDisplayMap(this, AssetFilter.Custodial, custodial, fiatRate)
        }

        if (interestEnabled && (interest as? Details.DetailsItem)?.isEnabled == true) {
            addToDisplayMap(this, AssetFilter.Interest, interest, fiatRate, interestRate)
        }
    }

    private fun addToDisplayMap(
        map: MutableMap<AssetFilter, AssetDisplayInfo>,
        filter: AssetFilter,
        item: Details,
        fiatRate: ExchangeRate,
        interestRate: Double = Double.NaN
    ) {
        (item as? Details.DetailsItem)?.let {
            val fiat = fiatRate.convert(it.balance)
            map.put(
                filter,
                AssetDisplayInfo(
                    account = it.account,
                    amount = it.balance,
                    fiatValue = fiat,
                    pendingAmount = it.pendingBalance,
                    actions = it.actions.filter { action ->
                        action != AssetAction.InterestDeposit
                    }.toSet(),
                    interestRate = interestRate
                )
            )
        }
    }

    fun loadRecurringBuysForAsset(assetTicker: String) =
        custodialWalletManager.getRecurringBuysForAsset(assetTicker)
}