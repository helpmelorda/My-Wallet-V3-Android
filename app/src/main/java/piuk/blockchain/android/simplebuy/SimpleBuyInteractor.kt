package piuk.blockchain.android.simplebuy

import com.blockchain.nabu.datamanagers.BillingAddress
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.BuySellPairs
import com.blockchain.nabu.datamanagers.CardToBeActivated
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.EligiblePaymentMethodType
import com.blockchain.nabu.datamanagers.OrderInput
import com.blockchain.nabu.datamanagers.OrderOutput
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.RecurringBuyOrder
import com.blockchain.nabu.datamanagers.SimpleBuyEligibilityProvider
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.nabu.models.data.BankPartner
import com.blockchain.nabu.models.data.LinkedBank
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.responses.banktransfer.ProviderAccountAttrs
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyRequestBody
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyConfirmationAttributes
import com.blockchain.nabu.service.TierService
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.BankLinkingPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.android.cards.CardIntent
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.networking.PollResult
import piuk.blockchain.android.networking.PollService
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.ui.linkbank.BankAuthDeepLinkState
import piuk.blockchain.android.ui.linkbank.BankAuthFlowState
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.ui.linkbank.BankTransferAction
import piuk.blockchain.android.ui.linkbank.fromPreferencesValue
import piuk.blockchain.android.ui.linkbank.toPreferencesValue
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.trackProgress
import java.util.concurrent.TimeUnit

class SimpleBuyInteractor(
    private val tierService: TierService,
    private val custodialWalletManager: CustodialWalletManager,
    private val withdrawLocksRepository: WithdrawLocksRepository,
    private val appUtil: AppUtil,
    private val analytics: Analytics,
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider,
    private val eligibilityProvider: SimpleBuyEligibilityProvider,
    private val coincore: Coincore,
    private val bankLinkingPrefs: BankLinkingPrefs
) {

    // ignore limits when user is in tier 0
    fun fetchBuyLimitsAndSupportedCryptoCurrencies(
        targetCurrency: String
    ): Single<Pair<BuySellPairs, TransferLimits?>> =
        custodialWalletManager.getSupportedBuySellCryptoCurrencies(targetCurrency).zipWith(
            tierService.tiers()
        ).flatMap { (pairs, tier) ->
            if (tier.isInInitialState()) {
                Single.just(pairs to null)
            } else
                custodialWalletManager.getProductTransferLimits(targetCurrency, product = Product.BUY).map {
                    pairs to it
                }
        }.trackProgress(appUtil.activityIndicator)

    fun fetchSupportedFiatCurrencies(): Single<SimpleBuyIntent.SupportedCurrenciesUpdated> =
        custodialWalletManager.getSupportedFiatCurrencies()
            .map { SimpleBuyIntent.SupportedCurrenciesUpdated(it) }
            .trackProgress(appUtil.activityIndicator)

    fun cancelOrder(orderId: String): Completable {
        bankLinkingPrefs.setBankLinkingState(BankAuthDeepLinkState().toPreferencesValue())
        return custodialWalletManager.deleteBuyOrder(orderId)
    }

    fun createOrder(
        cryptoAsset: AssetInfo,
        amount: FiatValue,
        paymentMethodId: String? = null,
        paymentMethod: PaymentMethodType,
        isPending: Boolean
    ): Single<SimpleBuyIntent.OrderCreated> =
        custodialWalletManager.createOrder(
            custodialWalletOrder = CustodialWalletOrder(
                pair = "${cryptoAsset.ticker}-${amount.currencyCode}",
                action = "BUY",
                input = OrderInput(
                    amount.currencyCode, amount.toBigInteger().toString()
                ),
                output = OrderOutput(
                    cryptoAsset.ticker, null
                ),
                paymentMethodId = paymentMethodId,
                paymentType = paymentMethod.name
            ),
            stateAction = if (isPending) "pending" else null
        ).map {
            SimpleBuyIntent.OrderCreated(it)
        }

    fun createRecurringBuyOrder(
        state: SimpleBuyState,
        recurringBuyFrequency: RecurringBuyFrequency
    ): Single<RecurringBuyOrder> {
        return if (recurringBuyFrequency != RecurringBuyFrequency.ONE_TIME) {
            val asset = state.selectedCryptoAsset
            require(asset != null) { "createRecurringBuyOrder selected crypto is null" }
            require(state.order.amount != null) { "createRecurringBuyOrder amount is null" }
            require(state.selectedPaymentMethod != null) { "createRecurringBuyOrder selected payment method is null" }

            val amount = state.order.amount
            val paymentMethod = state.selectedPaymentMethod
            custodialWalletManager.createRecurringBuyOrder(
                RecurringBuyRequestBody(
                    inputValue = amount?.toBigInteger().toString(),
                    inputCurrency = amount?.currencyCode.toString(),
                    destinationCurrency = asset.ticker,
                    paymentMethod = paymentMethod.paymentMethodType.name,
                    period = recurringBuyFrequency.name,
                    paymentMethodId = paymentMethod.takeUnless { it.isFunds() }?.id
                )
            )
        } else {
            Single.just(RecurringBuyOrder())
        }
    }

    fun fetchWithdrawLockTime(
        paymentMethod: PaymentMethodType,
        fiatCurrency: String
    ): Single<SimpleBuyIntent.WithdrawLocksTimeUpdated> =
        withdrawLocksRepository.getWithdrawLockTypeForPaymentMethod(paymentMethod, fiatCurrency)
            .map {
                SimpleBuyIntent.WithdrawLocksTimeUpdated(it)
            }.onErrorReturn {
                SimpleBuyIntent.WithdrawLocksTimeUpdated()
            }

    fun fetchQuote(asset: AssetInfo?, amount: FiatValue?): Single<SimpleBuyIntent.QuoteUpdated> =
        custodialWalletManager.getQuote(
            asset = asset ?: throw IllegalStateException("Missing Cryptocurrency "),
            fiatCurrency = amount?.currencyCode ?: throw IllegalStateException("Missing FiatCurrency "),
            action = "BUY",
            currency = amount.currencyCode,
            amount = amount.toBigInteger().toString()
        ).map {
            SimpleBuyIntent.QuoteUpdated(it)
        }

    fun pollForKycState(): Single<SimpleBuyIntent.KycStateUpdated> =
        tierService.tiers()
            .flatMap {
                when {
                    it.isApprovedFor(KycTierLevel.GOLD) ->
                        eligibilityProvider.isEligibleForSimpleBuy(forceRefresh = true).map { eligible ->
                            if (eligible) {
                                SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)
                            } else {
                                SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_BUT_NOT_ELIGIBLE)
                            }
                        }
                    it.isRejectedForAny() -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.FAILED))
                    it.isInReviewForAny() -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.IN_REVIEW))
                    else -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
                }
            }.onErrorReturn {
                SimpleBuyIntent.KycStateUpdated(KycState.PENDING)
            }
            .repeatWhen { it.delay(INTERVAL, TimeUnit.SECONDS).zipWith(Flowable.range(0, RETRIES_SHORT)) }
            .takeUntil { it.kycState != KycState.PENDING }
            .last(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
            .map {
                if (it.kycState == KycState.PENDING) {
                    SimpleBuyIntent.KycStateUpdated(KycState.UNDECIDED)
                } else {
                    it
                }
            }

    fun updateSelectedBankAccountId(
        linkingId: String,
        providerAccountId: String = "",
        accountId: String,
        partner: BankPartner,
        action: BankTransferAction,
        source: BankAuthSource
    ): Completable {
        bankLinkingPrefs.setBankLinkingState(
            BankAuthDeepLinkState(
                bankAuthFlow = BankAuthFlowState.BANK_LINK_PENDING,
                bankLinkingInfo = BankLinkingInfo(linkingId, source)
            ).toPreferencesValue()
        )

        return custodialWalletManager.updateSelectedBankAccount(
            linkingId = linkingId,
            providerAccountId = providerAccountId,
            accountId = accountId,
            attributes = providerAttributes(
                partner = partner,
                action = action,
                providerAccountId = providerAccountId,
                accountId = accountId
            )
        )
    }

    private fun providerAttributes(
        partner: BankPartner,
        providerAccountId: String,
        accountId: String,
        action: BankTransferAction
    ): ProviderAccountAttrs =
        when (partner) {
            BankPartner.YODLEE ->
                ProviderAccountAttrs(
                    providerAccountId = providerAccountId,
                    accountId = accountId
                )
            BankPartner.YAPILY ->
                ProviderAccountAttrs(
                    institutionId = accountId,
                    callback = bankPartnerCallbackProvider.callback(BankPartner.YAPILY, action)
                )
        }

    fun pollForBankLinkingCompleted(id: String): Single<LinkedBank> = PollService(
        custodialWalletManager.getLinkedBank(id)
    ) {
        it.isLinkingInFinishedState()
    }.start(timerInSec = INTERVAL, retries = RETRIES_DEFAULT).map {
        it.value
    }

    fun pollForLinkedBankState(id: String): Single<PollResult<LinkedBank>> = PollService(
        custodialWalletManager.getLinkedBank(id)
    ) {
        !it.isLinkingPending()
    }.start(timerInSec = INTERVAL, retries = RETRIES_DEFAULT)

    fun checkTierLevel(): Single<SimpleBuyIntent.KycStateUpdated> {

        return tierService.tiers().flatMap {
            when {
                it.isApprovedFor(KycTierLevel.GOLD) -> eligibilityProvider.isEligibleForSimpleBuy(forceRefresh = true)
                    .map { eligible ->
                        if (eligible) {
                            SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)
                        } else {
                            SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_BUT_NOT_ELIGIBLE)
                        }
                    }
                it.isRejectedFor(KycTierLevel.GOLD) -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.FAILED))
                it.isPendingFor(KycTierLevel.GOLD) -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.IN_REVIEW))
                else -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
            }
        }.onErrorReturn { SimpleBuyIntent.KycStateUpdated(KycState.PENDING) }
    }

    fun linkNewBank(fiatCurrency: String): Single<SimpleBuyIntent.BankLinkProcessStarted> {
        return custodialWalletManager.linkToABank(fiatCurrency).map {
            SimpleBuyIntent.BankLinkProcessStarted(
                it
            )
        }.trackProgress(appUtil.activityIndicator)
    }

    private fun KycTiers.isRejectedForAny(): Boolean =
        isRejectedFor(KycTierLevel.SILVER) ||
            isRejectedFor(KycTierLevel.GOLD)

    private fun KycTiers.isInReviewForAny(): Boolean =
        isUnderReviewFor(KycTierLevel.SILVER) ||
            isUnderReviewFor(KycTierLevel.GOLD)

    fun exchangeRate(asset: AssetInfo): Single<SimpleBuyIntent.ExchangePriceWithDeltaUpdated> =
        coincore.getExchangePriceWithDelta(asset)
            .map { exchangePriceWithDelta ->
                SimpleBuyIntent.ExchangePriceWithDeltaUpdated(exchangePriceWithDelta = exchangePriceWithDelta)
            }

    fun eligiblePaymentMethods(fiatCurrency: String, preselectedId: String?):
        Single<SimpleBuyIntent.PaymentMethodsUpdated> =
        tierService.tiers().zipWith(custodialWalletManager.isSimplifiedDueDiligenceEligible().onErrorReturn { false }
            .doOnSuccess {
                if (it) {
                    analytics.logEventOnce(SDDAnalytics.SDD_ELIGIBLE)
                }
            })
            .flatMap { (tier, sddEligible) ->
                custodialWalletManager.fetchSuggestedPaymentMethod(
                    fiatCurrency = fiatCurrency,
                    fetchSddLimits = sddEligible && tier.isInInitialState(),
                    onlyEligible = tier.isInitialisedFor(KycTierLevel.GOLD)
                ).map { paymentMethods ->
                    SimpleBuyIntent.PaymentMethodsUpdated(
                        availablePaymentMethods = paymentMethods,
                        canLinkBank = paymentMethods.filterIsInstance<PaymentMethod.UndefinedBankTransfer>()
                            .firstOrNull()?.isEligible ?: false,
                        canAddCard = paymentMethods.filterIsInstance<PaymentMethod.UndefinedCard>()
                            .firstOrNull()?.isEligible ?: false,
                        canLinkFunds = paymentMethods.filterIsInstance<PaymentMethod.UndefinedBankAccount>()
                            .firstOrNull()?.isEligible ?: false,
                        preselectedId = preselectedId
                    )
                }
            }

    fun getRecurringBuyEligibility() = custodialWalletManager.getRecurringBuyEligibility()

    // attributes are null in case of bank
    fun confirmOrder(
        orderId: String,
        paymentMethodId: String?,
        attributes: SimpleBuyConfirmationAttributes?,
        isBankPartner: Boolean?
    ): Single<BuySellOrder> = custodialWalletManager.confirmOrder(orderId, attributes, paymentMethodId, isBankPartner)

    fun pollForOrderStatus(orderId: String): Single<BuySellOrder> =
        custodialWalletManager.getBuyOrder(orderId)
            .repeatWhen { it.delay(INTERVAL, TimeUnit.SECONDS).zipWith(Flowable.range(0, RETRIES_LONG)) }
            .takeUntil {
                it.state == OrderState.FINISHED ||
                    it.state == OrderState.FAILED ||
                    it.state == OrderState.CANCELED
            }.lastOrError()

    fun pollForAuthorisationUrl(orderId: String): Single<PollResult<BuySellOrder>> =
        PollService(
            custodialWalletManager.getBuyOrder(orderId)
        ) {
            it.attributes?.authorisationUrl != null
        }.start()

    fun pollForCardStatus(cardId: String): Single<CardIntent.CardUpdated> =
        PollService(
            custodialWalletManager.getCardDetails(cardId)
        ) {
            it.status == CardStatus.BLOCKED ||
                it.status == CardStatus.EXPIRED ||
                it.status == CardStatus.ACTIVE
        }
            .start()
            .map {
                CardIntent.CardUpdated(it.value)
            }

    fun eligiblePaymentMethodsTypes(fiatCurrency: String): Single<List<EligiblePaymentMethodType>> =
        custodialWalletManager.getEligiblePaymentMethodTypes(
            fiatCurrency = fiatCurrency
        )

    fun getLinkedBankInfo(paymentMethodId: String) =
        custodialWalletManager.getLinkedBank(paymentMethodId)

    fun fetchOrder(orderId: String) = custodialWalletManager.getBuyOrder(orderId)

    fun addNewCard(fiatCurrency: String, billingAddress: BillingAddress): Single<CardToBeActivated> =
        custodialWalletManager.addNewCard(fiatCurrency, billingAddress)

    fun updateApprovalStatus() {
        val currentState = bankLinkingPrefs.getBankLinkingState().fromPreferencesValue()
        bankLinkingPrefs.setBankLinkingState(
            currentState.copy(bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_PENDING).toPreferencesValue()
        )
    }

    fun updateOneTimeTokenPath(callbackPath: String) {
        val sanitisedUrl = callbackPath.removePrefix("nabu-gateway/")
        bankLinkingPrefs.setDynamicOneTimeTokenUrl(sanitisedUrl)
    }

    companion object {
        private const val INTERVAL: Long = 5
        private const val RETRIES_SHORT = 6
        private const val RETRIES_DEFAULT = 12
        private const val RETRIES_LONG = 20
    }
}
