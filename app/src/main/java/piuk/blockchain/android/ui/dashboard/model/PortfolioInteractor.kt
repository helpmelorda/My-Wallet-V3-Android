package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.core.price.ExchangeRates
import com.blockchain.core.price.HistoricalRate
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.balance.isErc20
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.coincore.fiat.LinkedBanksFactory
import piuk.blockchain.android.coincore.toFiat
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsFlow
import piuk.blockchain.android.ui.settings.LinkablePaymentMethods
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import timber.log.Timber
import java.util.concurrent.TimeUnit

private class DashboardGroupLoadFailure(msg: String, e: Throwable) : Exception(msg, e)
private class DashboardBalanceLoadFailure(msg: String, e: Throwable) : Exception(msg, e)

class PortfolioInteractor(
    private val coincore: Coincore,
    private val payloadManager: PayloadDataManager,
    private val exchangeRates: ExchangeRates,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val analytics: Analytics,
    private val crashLogger: CrashLogger,
    private val linkedBanksFactory: LinkedBanksFactory,
    @Suppress("unused")
    private val gatedFeatures: InternalFeatureFlagApi
) {

    // We have a problem here, in that pax init depends on ETH init
    // Ultimately, we want to init metadata straight after decrypting (or creating) the wallet
    // but we can't move that somewhere sensible yet, because 2nd password. When we remove that -
    // which is on the radar - then we can clean up the entire app init sequence.
    // But for now, we'll catch any pax init failure here, unless ETH has initialised OK. And when we
    // get a valid ETH balance, will try for a PX balance. Yeah, this is a nasty hack TODO: Fix this
    fun refreshBalances(
        model: PortfolioModel,
        balanceFilter: AssetFilter,
        state: PortfolioState
    ): Disposable {
        val cd = CompositeDisposable()

        state.assetMapKeys
            .filter { !it.isErc20() }
            .forEach { asset ->
                cd += refreshAssetBalance(asset, model, balanceFilter)
                    .ifEthLoadedGetErc20Balance(model, balanceFilter, cd, state)
                    .ifEthFailedThenErc20Failed(asset, model, state)
                    .emptySubscribe()
            }

        cd += checkForFiatBalances(model, currencyPrefs.selectedFiatCurrency)

        return cd
    }

    fun getAvailableAssets(model: PortfolioModel): Disposable =
        Single.fromCallable {
            if (gatedFeatures.isFeatureEnabled(GatedFeature.NEW_SPLIT_DASHBOARD)) {
                coincore.fundedCryptoAssets()
            } else {
                coincore.availableCryptoAssets()
            }
        }.subscribeBy(
            onSuccess = { assets ->
                model.process(UpdatePortfolioCurrencies(assets))
                model.process(RefreshAllIntent)
            },
            onError = {
                Timber.e("Error getting ordering - $it")
            }
        )

    private fun refreshAssetBalance(
        asset: AssetInfo,
        model: PortfolioModel,
        balanceFilter: AssetFilter
    ): Single<CryptoValue> =
        coincore[asset].accountGroup(balanceFilter)
            .logGroupLoadError(asset, balanceFilter)
            .flatMapSingle { group ->
                group.accountBalance
                    .logBalanceLoadError(asset, balanceFilter)
            }
            .map { balance -> balance as CryptoValue }
            .doOnError { e ->
                Timber.e("Failed getting balance for ${asset.ticker}: $e")
                model.process(BalanceUpdateError(asset))
            }
            .doOnSuccess { v ->
                Timber.d("Got balance for ${asset.ticker}")
                model.process(BalanceUpdate(asset, v))
            }.defaultIfEmpty(CryptoValue.zero(asset))
            .retryOnError()

    private fun <T> Single<T>.retryOnError() =
        this.retryWhen { f ->
            f.take(RETRY_COUNT)
                .delay(RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS)
        }

    private fun Single<CryptoValue>.ifEthLoadedGetErc20Balance(
        model: PortfolioModel,
        balanceFilter: AssetFilter,
        disposables: CompositeDisposable,
        state: PortfolioState
    ) = this.doOnSuccess { value ->
        if (value.currency == CryptoCurrency.ETHER) {
            state.erc20Assets.forEach {
                disposables += refreshAssetBalance(it, model, balanceFilter)
                    .emptySubscribe()
            }
        }
    }

    private fun Single<CryptoValue>.ifEthFailedThenErc20Failed(
        asset: AssetInfo,
        model: PortfolioModel,
        state: PortfolioState
    ) = this.doOnError {
        if (asset.ticker == CryptoCurrency.ETHER.ticker) {
            state.erc20Assets.forEach {
                model.process(BalanceUpdateError(it))
            }
        }
    }

    private fun Maybe<AccountGroup>.logGroupLoadError(asset: AssetInfo, filter: AssetFilter) =
        this.doOnError { e ->
            crashLogger.logException(
                DashboardGroupLoadFailure("Cannot load group for ${asset.ticker} - $filter:", e)
            )
        }

    private fun Single<Money>.logBalanceLoadError(asset: AssetInfo, filter: AssetFilter) =
        this.doOnError { e ->
            crashLogger.logException(
                DashboardBalanceLoadFailure("Cannot load balance for ${asset.ticker} - $filter:", e)
            )
        }

    private fun checkForFiatBalances(model: PortfolioModel, fiatCurrency: String): Disposable =
        coincore.fiatAssets.accountGroup()
            .flattenAsObservable { g -> g.accounts }
            .flatMapSingle { a ->
                a.accountBalance.map { balance ->
                    FiatBalanceInfo(
                        balance,
                        balance.toFiat(fiatCurrency, exchangeRates),
                        a as FiatAccount
                    )
                }
            }
            .toList()
            .subscribeBy(
                onSuccess = { balances ->
                    if (balances.isNotEmpty()) {
                        model.process(FiatBalanceUpdate(balances))
                    }
                },
                onError = {
                    Timber.e("Error while loading fiat balances $it")
                }
            )

    fun refreshPrices(model: PortfolioModel, crypto: AssetInfo): Disposable {
        return Single.zip(
            coincore[crypto].exchangeRate(),
            coincore[crypto].getPricesWith24hDelta()
        ) { rate, delta -> PriceUpdate(crypto, rate, delta) }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = { Timber.e(it) }
            )
    }

    fun refreshPriceHistory(model: PortfolioModel, asset: AssetInfo): Disposable =
        if (asset.startDate != null) {
            coincore[asset].lastDayTrend()
        } else {
            Single.just(FLATLINE_CHART)
        }.map { PriceHistoryUpdate(asset, it) }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = { Timber.e(it) }
            )

    fun checkForCustodialBalance(model: PortfolioModel, crypto: AssetInfo): Disposable {
        return coincore[crypto].accountGroup(AssetFilter.Custodial)
            .flatMapObservable { it.balance }
            .subscribeBy(
                onNext = {
                    model.process(UpdateHasCustodialBalanceIntent(crypto, !it.total.isZero))
                },
                onError = { model.process(UpdateHasCustodialBalanceIntent(crypto, false)) }
            )
    }

    fun hasUserBackedUp(): Single<Boolean> = Single.just(payloadManager.isBackedUp)

    fun cancelSimpleBuyOrder(orderId: String): Disposable {
        return custodialWalletManager.deleteBuyOrder(orderId)
            .subscribeBy(
                onComplete = { simpleBuyPrefs.clearBuyState() },
                onError = { error ->
                    analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_ERROR)
                    Timber.e(error)
                }
            )
    }

    fun getSendFlow(
        model: PortfolioModel,
        fromAccount: SingleAccount,
        action: AssetAction
    ): Disposable? {
        if (fromAccount is CryptoAccount) {
            model.process(
                UpdateLaunchDialogFlow(
                    TransactionFlow(
                        sourceAccount = fromAccount,
                        action = action
                    )
                )
            )
        }
        return null
    }

    fun getAssetDetailsFlow(model: PortfolioModel, asset: AssetInfo): Disposable? {
        model.process(
            UpdateLaunchDialogFlow(
                AssetDetailsFlow(
                    asset = asset,
                    coincore = coincore
                )
            )
        )
        return null
    }

    fun getInterestDepositFlow(
        model: PortfolioModel,
        targetAccount: InterestAccount
    ): Disposable? {
        require(targetAccount is CryptoAccount)
        model.process(
            UpdateLaunchDialogFlow(
                TransactionFlow(
                    target = targetAccount,
                    action = AssetAction.InterestDeposit
                )
            )
        )
        return null
    }

    fun getInterestWithdrawFlow(
        model: PortfolioModel,
        sourceAccount: InterestAccount
    ): Disposable? {
        require(sourceAccount is CryptoAccount)

        model.process(
            UpdateLaunchDialogFlow(
                TransactionFlow(
                    sourceAccount = sourceAccount,
                    action = AssetAction.InterestWithdraw
                )
            )
        )
        return null
    }

    fun getBankDepositFlow(
        model: PortfolioModel,
        targetAccount: SingleAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean
    ): Disposable {
        require(targetAccount is FiatAccount)
        return handleFiatDeposit(targetAccount, shouldLaunchBankLinkTransfer, model, action)
    }

    private fun handleFiatDeposit(
        targetAccount: FiatAccount,
        shouldLaunchBankLinkTransfer: Boolean,
        model: PortfolioModel,
        action: AssetAction
    ) = Singles.zip(
        linkedBanksFactory.eligibleBankPaymentMethods(targetAccount.fiatCurrency).map { paymentMethods ->
            // Ignore any WireTransferMethods In case BankLinkTransfer should launch
            paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
        },
        linkedBanksFactory.getNonWireTransferBanks().map {
            it.filter { bank -> bank.currency == targetAccount.fiatCurrency }
        }
    ).doOnSubscribe {
        model.process(LongCallStarted)
    }.flatMap { (paymentMethods, linkedBanks) ->
        when {
            linkedBanks.isEmpty() -> {
                handleNoLinkedBanks(
                    targetAccount,
                    LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit(
                        linkablePaymentMethods = LinkablePaymentMethods(
                            targetAccount.fiatCurrency,
                            paymentMethods
                        )
                    )
                )
            }
            linkedBanks.size == 1 -> {
                Single.just(FiatTransactionRequestResult.LaunchDepositFlow(linkedBanks[0]))
            }
            else -> {
                Single.just(FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts)
            }
        }
    }.doOnTerminate {
        model.process(LongCallEnded)
    }.subscribeBy(
        onSuccess = {
            handlePaymentMethodsUpdate(it, model, targetAccount, action)
        },
        onError = {
            Timber.e("Error loading bank transfer info $it")
        }
    )

    private fun handlePaymentMethodsUpdate(
        it: FiatTransactionRequestResult?,
        model: PortfolioModel,
        fiatAccount: SingleAccount,
        action: AssetAction
    ) {
        when (it) {
            is FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts -> {
                model.process(
                    UpdateLaunchDialogFlow(
                        TransactionFlow(
                            target = fiatAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchDepositFlow -> {
                model.process(
                    UpdateLaunchDialogFlow(
                        TransactionFlow(
                            target = fiatAccount,
                            sourceAccount = it.preselectedBankAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchWithdrawalFlowWithMultipleAccounts -> {
                model.process(
                    UpdateLaunchDialogFlow(
                        TransactionFlow(
                            sourceAccount = fiatAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchWithdrawalFlow -> {
                model.process(
                    UpdateLaunchDialogFlow(
                        TransactionFlow(
                            sourceAccount = fiatAccount,
                            target = it.preselectedBankAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchBankLink -> {
                model.process(
                    LaunchBankLinkFlow(
                        it.linkBankTransfer,
                        action
                    )
                )
            }
            is FiatTransactionRequestResult.NotSupportedPartner -> {
                // TODO Show an error
            }
            is FiatTransactionRequestResult.LaunchPaymentMethodChooser -> {
                model.process(
                    ShowLinkablePaymentMethodsSheet(it.paymentMethodForAction)
                )
            }
            is FiatTransactionRequestResult.LaunchDepositDetailsSheet -> {
                model.process(ShowBankLinkingSheet(it.targetAccount))
            }
        }
    }

    private fun handleNoLinkedBanks(
        targetAccount: FiatAccount,
        paymentMethodForAction: LinkablePaymentMethodsForAction
    ) =
        when {
            paymentMethodForAction.linkablePaymentMethods.linkMethods.containsAll(
                listOf(PaymentMethodType.BANK_TRANSFER, PaymentMethodType.BANK_ACCOUNT)
            ) -> {
                Single.just(
                    FiatTransactionRequestResult.LaunchPaymentMethodChooser(
                        paymentMethodForAction
                    )
                )
            }
            paymentMethodForAction.linkablePaymentMethods.linkMethods.contains(PaymentMethodType.BANK_TRANSFER) -> {
                linkBankTransfer(targetAccount.fiatCurrency).map {
                    FiatTransactionRequestResult.LaunchBankLink(it) as FiatTransactionRequestResult
                }.onErrorReturn {
                    FiatTransactionRequestResult.NotSupportedPartner
                }
            }
            paymentMethodForAction.linkablePaymentMethods.linkMethods.contains(PaymentMethodType.BANK_ACCOUNT) -> {
                Single.just(FiatTransactionRequestResult.LaunchDepositDetailsSheet(targetAccount))
            }
            else -> {
                Single.just(FiatTransactionRequestResult.NotSupportedPartner)
            }
        }

    fun linkBankTransfer(currency: String): Single<LinkBankTransfer> =
        custodialWalletManager.linkToABank(currency)

    fun getBankWithdrawalFlow(
        model: PortfolioModel,
        sourceAccount: SingleAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean
    ): Disposable {
        require(sourceAccount is FiatAccount)

        return Singles.zip(
            linkedBanksFactory.eligibleBankPaymentMethods(sourceAccount.fiatCurrency).map { paymentMethods ->
                // Ignore any WireTransferMethods In case BankLinkTransfer should launch
                paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
            },
            linkedBanksFactory.getAllLinkedBanks().map {
                it.filter { bank -> bank.currency == sourceAccount.fiatCurrency }
            }
        ).flatMap { (paymentMethods, linkedBanks) ->
            when {
                linkedBanks.isEmpty() -> {
                    handleNoLinkedBanks(
                        sourceAccount,
                        LinkablePaymentMethodsForAction.LinkablePaymentMethodsForWithdraw(
                            LinkablePaymentMethods(
                                sourceAccount.fiatCurrency,
                                paymentMethods
                            )
                        )
                    )
                }
                linkedBanks.size == 1 -> {
                    Single.just(FiatTransactionRequestResult.LaunchWithdrawalFlow(linkedBanks[0]))
                }
                else -> {
                    Single.just(FiatTransactionRequestResult.LaunchWithdrawalFlowWithMultipleAccounts)
                }
            }
        }.subscribeBy(
            onSuccess = {
                handlePaymentMethodsUpdate(it, model, sourceAccount, action)
            },
            onError = {
                // TODO Add error state to Dashboard
            }
        )
    }

    companion object {
        private val FLATLINE_CHART = listOf(
            HistoricalRate(rate = 1.0, timestamp = 0),
            HistoricalRate(rate = 1.0, timestamp = System.currentTimeMillis() / 1000)
        )

        private const val RETRY_INTERVAL_MS = 3000L
        private const val RETRY_COUNT = 3L
    }
}

private sealed class FiatTransactionRequestResult {
    class LaunchBankLink(val linkBankTransfer: LinkBankTransfer) : FiatTransactionRequestResult()
    class LaunchDepositFlow(val preselectedBankAccount: LinkedBankAccount) : FiatTransactionRequestResult()
    class LaunchPaymentMethodChooser(val paymentMethodForAction: LinkablePaymentMethodsForAction) :
        FiatTransactionRequestResult()

    class LaunchDepositDetailsSheet(val targetAccount: FiatAccount) : FiatTransactionRequestResult()
    object LaunchDepositFlowWithMultipleAccounts : FiatTransactionRequestResult()
    class LaunchWithdrawalFlow(val preselectedBankAccount: LinkedBankAccount) : FiatTransactionRequestResult()
    object LaunchWithdrawalFlowWithMultipleAccounts : FiatTransactionRequestResult()
    object NotSupportedPartner : FiatTransactionRequestResult()
}
