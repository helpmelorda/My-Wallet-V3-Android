package com.blockchain.nabu.service

import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.extensions.wrapErrorMessage
import com.blockchain.nabu.models.responses.banktransfer.BankTransferPaymentBody
import com.blockchain.nabu.models.responses.banktransfer.CreateLinkBankRequestBody
import com.blockchain.nabu.models.responses.banktransfer.OpenBankingTokenBody
import com.blockchain.nabu.models.responses.banktransfer.UpdateProviderAccountBody
import com.blockchain.nabu.models.responses.interest.InterestWithdrawalBody
import com.blockchain.nabu.models.responses.nabu.AddAddressRequest
import com.blockchain.nabu.models.responses.nabu.AirdropStatusList
import com.blockchain.nabu.models.responses.nabu.ApplicantIdRequest
import com.blockchain.nabu.models.responses.nabu.NabuBasicUser
import com.blockchain.nabu.models.responses.nabu.NabuCountryResponse
import com.blockchain.nabu.models.responses.nabu.NabuJwt
import com.blockchain.nabu.models.responses.nabu.NabuRecoverAccountRequest
import com.blockchain.nabu.models.responses.nabu.NabuRecoverAccountResponse
import com.blockchain.nabu.models.responses.nabu.NabuStateResponse
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.RecordCountryRequest
import com.blockchain.nabu.models.responses.nabu.RegisterCampaignRequest
import com.blockchain.nabu.models.responses.nabu.Scope
import com.blockchain.nabu.models.responses.nabu.SendToMercuryAddressRequest
import com.blockchain.nabu.models.responses.nabu.SendToMercuryAddressResponse
import com.blockchain.nabu.models.responses.nabu.SendWithdrawalAddressesRequest
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.nabu.models.responses.nabu.WalletMercuryLink
import com.blockchain.nabu.models.responses.sdd.SDDEligibilityResponse
import com.blockchain.nabu.models.responses.sdd.SDDStatusResponse
import com.blockchain.nabu.models.responses.simplebuy.AddNewCardBodyRequest
import com.blockchain.nabu.models.responses.simplebuy.BankAccountResponse
import com.blockchain.nabu.models.responses.simplebuy.ConfirmOrderRequestBody
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.nabu.models.responses.simplebuy.DepositRequestBody
import com.blockchain.nabu.models.responses.simplebuy.ProductTransferRequestBody
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyRequestBody
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyConfirmationAttributes
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyCurrency
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibility
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairsResp
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyQuoteResponse
import com.blockchain.nabu.models.responses.simplebuy.TransactionsResponse
import com.blockchain.nabu.models.responses.simplebuy.TransferFundsResponse
import com.blockchain.nabu.models.responses.simplebuy.TransferRequest
import com.blockchain.nabu.models.responses.simplebuy.WithdrawLocksCheckRequestBody
import com.blockchain.nabu.models.responses.simplebuy.WithdrawRequestBody
import com.blockchain.nabu.models.responses.swap.CreateOrderRequest
import com.blockchain.nabu.models.responses.swap.CustodialOrderResponse
import com.blockchain.nabu.models.responses.swap.QuoteRequest
import com.blockchain.nabu.models.responses.swap.QuoteResponse
import com.blockchain.nabu.models.responses.swap.SwapLimitsResponse
import com.blockchain.nabu.models.responses.swap.UpdateSwapOrderBody
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenRequest
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.veriff.VeriffApplicantAndToken
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import retrofit2.HttpException

class NabuService internal constructor (
    private val nabu: Nabu
) {
    internal fun getAuthToken(
        jwt: String,
        currency: String? = null,
        action: String? = null
    ): Single<NabuOfflineTokenResponse> = nabu.getAuthToken(
        jwt = NabuOfflineTokenRequest(jwt), currency = currency, action = action
    ).wrapErrorMessage()

    internal fun getSessionToken(
        userId: String,
        offlineToken: String,
        guid: String,
        email: String,
        appVersion: String,
        deviceId: String
    ): Single<NabuSessionTokenResponse> = nabu.getSessionToken(
        userId,
        offlineToken,
        guid,
        email,
        appVersion,
        CLIENT_TYPE,
        deviceId
    ).wrapErrorMessage()

    internal fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        sessionToken: NabuSessionTokenResponse
    ): Completable = nabu.createBasicUser(
        NabuBasicUser(firstName, lastName, dateOfBirth),
        sessionToken.authHeader
    )

    internal fun getUser(
        sessionToken: NabuSessionTokenResponse
    ): Single<NabuUser> = nabu.getUser(
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun getAirdropCampaignStatus(
        sessionToken: NabuSessionTokenResponse
    ): Single<AirdropStatusList> = nabu.getAirdropCampaignStatus(
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun updateWalletInformation(
        sessionToken: NabuSessionTokenResponse,
        jwt: String
    ): Single<NabuUser> = nabu.updateWalletInformation(
        NabuJwt(jwt),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun getCountriesList(
        scope: Scope
    ): Single<List<NabuCountryResponse>> = nabu.getCountriesList(
        scope.value
    ).wrapErrorMessage()

    internal fun getStatesList(
        countryCode: String,
        scope: Scope
    ): Single<List<NabuStateResponse>> = nabu.getStatesList(
        countryCode,
        scope.value
    ).wrapErrorMessage()

    internal fun getSupportedDocuments(
        sessionToken: NabuSessionTokenResponse,
        countryCode: String
    ): Single<List<SupportedDocuments>> = nabu.getSupportedDocuments(
        countryCode,
        sessionToken.authHeader
    ).wrapErrorMessage()
        .map { it.documentTypes }

    internal fun addAddress(
        sessionToken: NabuSessionTokenResponse,
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable = nabu.addAddress(
        AddAddressRequest.fromAddressDetails(
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        ),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun recordCountrySelection(
        sessionToken: NabuSessionTokenResponse,
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable = nabu.recordSelectedCountry(
        RecordCountryRequest(
            jwt,
            countryCode,
            notifyWhenAvailable,
            stateCode
        ),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun startVeriffSession(
        sessionToken: NabuSessionTokenResponse
    ): Single<VeriffApplicantAndToken> = nabu.startVeriffSession(
        sessionToken.authHeader
    ).map { VeriffApplicantAndToken(it.applicantId, it.token) }
        .wrapErrorMessage()

    internal fun submitVeriffVerification(
        sessionToken: NabuSessionTokenResponse
    ): Completable = nabu.submitVerification(
        ApplicantIdRequest(sessionToken.userId),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun recoverAccount(
        offlineToken: NabuOfflineTokenResponse,
        jwt: String,
        recoveryToken: String
    ): Single<NabuRecoverAccountResponse> = nabu.recoverAccount(
        offlineToken.userId,
        NabuRecoverAccountRequest(
            jwt = jwt,
            recoveryToken = recoveryToken
        ),
        authorization = "Bearer ${offlineToken.token}"
    ).wrapErrorMessage()

    internal fun recoverUser(
        offlineToken: NabuOfflineTokenResponse,
        jwt: String
    ): Completable = nabu.recoverUser(
        offlineToken.userId,
        NabuJwt(jwt),
        authorization = "Bearer ${offlineToken.token}"
    ).wrapErrorMessage()

    internal fun resetUserKyc(
        offlineToken: NabuOfflineTokenResponse,
        jwt: String
    ): Completable = nabu.resetUserKyc(
        offlineToken.userId,
        NabuJwt(jwt),
        authorization = "Bearer ${offlineToken.token}"
    ).wrapErrorMessage()

    internal fun registerCampaign(
        sessionToken: NabuSessionTokenResponse,
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable = nabu.registerCampaign(
        campaignRequest,
        campaignName,
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun linkWalletWithMercury(
        sessionToken: NabuSessionTokenResponse
    ): Single<String> = nabu.connectWalletWithMercury(
        sessionToken.authHeader
    ).map { it.linkId }
        .wrapErrorMessage()

    internal fun linkMercuryWithWallet(
        sessionToken: NabuSessionTokenResponse,
        linkId: String
    ): Completable = nabu.connectMercuryWithWallet(
        sessionToken.authHeader,
        WalletMercuryLink(linkId)
    ).wrapErrorMessage()

    internal fun sendWalletAddressesToThePit(
        sessionToken: NabuSessionTokenResponse,
        request: SendWithdrawalAddressesRequest
    ): Completable = nabu.sharePitReceiveAddresses(
        sessionToken.authHeader,
        request
    ).wrapErrorMessage()

    internal fun fetchPitSendToAddressForCrypto(
        sessionToken: NabuSessionTokenResponse,
        cryptoSymbol: String
    ): Single<SendToMercuryAddressResponse> = nabu.fetchPitSendAddress(
        sessionToken.authHeader,
        SendToMercuryAddressRequest(cryptoSymbol)
    ).wrapErrorMessage()

    internal fun isSDDEligible(): Single<SDDEligibilityResponse> =
        nabu.isSDDEligible().wrapErrorMessage()

    internal fun isSDDVerified(sessionToken: NabuSessionTokenResponse): Single<SDDStatusResponse> =
        nabu.isSDDVerified(
            sessionToken.authHeader
        ).wrapErrorMessage()

    internal fun fetchQuote(
        sessionToken: NabuSessionTokenResponse,
        quoteRequest: QuoteRequest
    ): Single<QuoteResponse> = nabu.fetchQuote(
        sessionToken.authHeader,
        quoteRequest
    ).wrapErrorMessage()

    internal fun createCustodialOrder(
        sessionToken: NabuSessionTokenResponse,
        createOrderRequest: CreateOrderRequest
    ): Single<CustodialOrderResponse> = nabu.createCustodialOrder(
        sessionToken.authHeader,
        createOrderRequest
    ).wrapErrorMessage()

    internal fun fetchProductLimits(
        sessionToken: NabuSessionTokenResponse,
        currency: String,
        product: String,
        side: String?,
        orderDirection: String?
    ): Single<SwapLimitsResponse> = nabu.fetchLimits(
        authorization = sessionToken.authHeader,
        currency = currency,
        product = product,
        side = side,
        orderDirection = orderDirection
    ).onErrorResumeNext {
        if ((it as? HttpException)?.code() == 409) {
            Single.just(
                SwapLimitsResponse()
            )
        } else {
            Single.error(it)
        }
    }.wrapErrorMessage()

    internal fun fetchSwapActivity(
        sessionToken: NabuSessionTokenResponse
    ): Single<List<CustodialOrderResponse>> =
        nabu.fetchSwapActivity(sessionToken.authHeader).wrapErrorMessage()

    internal fun getSupportedCurrencies(
        fiatCurrency: String? = null
    ): Single<SimpleBuyPairsResp> =
        nabu.getSupportedSimpleBuyPairs(fiatCurrency).wrapErrorMessage()

    fun getSimpleBuyBankAccountDetails(
        sessionToken: NabuSessionTokenResponse,
        currency: String
    ): Single<BankAccountResponse> =
        nabu.getSimpleBuyBankAccountDetails(
            sessionToken.authHeader, SimpleBuyCurrency(currency)
        ).wrapErrorMessage()

    internal fun getSimpleBuyQuote(
        sessionToken: NabuSessionTokenResponse,
        action: String,
        currencyPair: String,
        currency: String,
        amount: String
    ): Single<SimpleBuyQuoteResponse> = nabu.getSimpleBuyQuote(
        authorization = sessionToken.authHeader,
        action = action,
        currencyPair = currencyPair,
        currency = currency,
        amount = amount
    )

    internal fun getPredefinedAmounts(
        sessionToken: NabuSessionTokenResponse,
        currency: String
    ): Single<List<Map<String, List<Long>>>> = nabu.getPredefinedAmounts(
        sessionToken.authHeader,
        currency
    ).wrapErrorMessage()

    internal fun getTransactions(
        sessionToken: NabuSessionTokenResponse,
        currency: String,
        product: String,
        type: String?
    ): Single<TransactionsResponse> = nabu.getTransactions(
        sessionToken.authHeader,
        currency,
        product,
        type
    ).wrapErrorMessage()

    internal fun isEligibleForSimpleBuy(
        sessionToken: NabuSessionTokenResponse,
        fiatCurrency: String
    ): Single<SimpleBuyEligibility> = nabu.isEligibleForSimpleBuy(
        sessionToken.authHeader,
        fiatCurrency
    ).wrapErrorMessage()

    internal fun createOrder(
        sessionToken: NabuSessionTokenResponse,
        order: CustodialWalletOrder,
        action: String?
    ) = nabu.createOrder(
        authorization = sessionToken.authHeader, action = action, order = order
    ).onErrorResumeNext {
        if (it is HttpException && it.code() == 409) {
            Single.error(TransactionError.OrderLimitReached)
        } else {
            Single.error(it)
        }
    }.wrapErrorMessage()

    fun createRecurringBuyOrder(
        sessionToken: NabuSessionTokenResponse,
        recurringOrderBody: RecurringBuyRequestBody
    ) = nabu.createRecurringBuy(
        authorization = sessionToken.authHeader,
        recurringBuyBody = recurringOrderBody
    ).wrapErrorMessage()

    internal fun fetchWithdrawFeesAndLimits(
        sessionToken: NabuSessionTokenResponse,
        product: String,
        paymentMethod: String
    ) = nabu.getWithdrawFeeAndLimits(
        sessionToken.authHeader, product, paymentMethod
    ).wrapErrorMessage()

    internal fun fetchWithdrawLocksRules(
        sessionToken: NabuSessionTokenResponse,
        paymentMethod: PaymentMethodType,
        fiatCurrency: String,
        productType: String
    ) = nabu.getWithdrawalLocksCheck(
        sessionToken.authHeader,
        WithdrawLocksCheckRequestBody(
            paymentMethod = paymentMethod.name, product = productType, currency = fiatCurrency
        )
    ).wrapErrorMessage()

    internal fun createWithdrawOrder(
        sessionToken: NabuSessionTokenResponse,
        amount: String,
        currency: String,
        beneficiaryId: String
    ) = nabu.withdrawOrder(
        sessionToken.authHeader,
        WithdrawRequestBody(beneficiary = beneficiaryId, amount = amount, currency = currency)
    ).wrapErrorMessage()

    internal fun createDepositTransaction(
        sessionToken: NabuSessionTokenResponse,
        currency: String,
        address: String,
        hash: String,
        amount: String,
        product: String
    ) = nabu.createDepositOrder(
        sessionToken.authHeader,
        DepositRequestBody(
            currency = currency, depositAddress = address, txHash = hash, amount = amount, product = product
        )
    )

    internal fun updateOrder(
        sessionToken: NabuSessionTokenResponse,
        id: String,
        success: Boolean
    ) = nabu.updateOrder(
        sessionToken.authHeader,
        id,
        UpdateSwapOrderBody.newInstance(success)
    ).wrapErrorMessage()

    internal fun getOutstandingOrders(
        sessionToken: NabuSessionTokenResponse,
        pendingOnly: Boolean
    ) = nabu.getOrders(
        sessionToken.authHeader,
        pendingOnly
    ).wrapErrorMessage()

    internal fun getSwapTrades(sessionToken: NabuSessionTokenResponse) = nabu.getSwapOrders(sessionToken.authHeader)

    internal fun getSwapAvailablePairs(sessionToken: NabuSessionTokenResponse) =
        nabu.getSwapAvailablePairs(sessionToken.authHeader)

    internal fun deleteBuyOrder(
        sessionToken: NabuSessionTokenResponse,
        orderId: String
    ) = nabu.deleteBuyOrder(
        sessionToken.authHeader, orderId
    ).onErrorResumeNext {
        if (it is HttpException && it.code() == 409) {
            Completable.error(TransactionError.OrderNotCancelable)
        } else {
            Completable.error(it)
        }
    }.wrapErrorMessage()

    fun getBuyOrder(
        sessionToken: NabuSessionTokenResponse,
        orderId: String
    ) = nabu.getBuyOrder(
        sessionToken.authHeader, orderId
    ).wrapErrorMessage()

    fun deleteCard(
        sessionToken: NabuSessionTokenResponse,
        cardId: String
    ) = nabu.deleteCard(
        sessionToken.authHeader, cardId
    ).wrapErrorMessage()

    fun removeBeneficiary(
        sessionToken: NabuSessionTokenResponse,
        id: String
    ) = nabu.removeBeneficiary(
        sessionToken.authHeader, id
    ).wrapErrorMessage()

    fun removeLinkedBank(
        sessionToken: NabuSessionTokenResponse,
        id: String
    ) = nabu.removeLinkedBank(
        sessionToken.authHeader, id
    ).wrapErrorMessage()

    fun addNewCard(
        sessionToken: NabuSessionTokenResponse,
        addNewCardBodyRequest: AddNewCardBodyRequest
    ) = nabu.addNewCard(
        sessionToken.authHeader, addNewCardBodyRequest
    ).wrapErrorMessage()

    fun activateCard(
        sessionToken: NabuSessionTokenResponse,
        cardId: String,
        attributes: SimpleBuyConfirmationAttributes
    ) = nabu.activateCard(
        sessionToken.authHeader, cardId, attributes
    ).wrapErrorMessage()

    fun getCardDetails(
        sessionToken: NabuSessionTokenResponse,
        cardId: String
    ) = nabu.getCardDetails(
        sessionToken.authHeader, cardId
    ).wrapErrorMessage()

    fun confirmOrder(
        sessionToken: NabuSessionTokenResponse,
        orderId: String,
        confirmBody: ConfirmOrderRequestBody
    ) = nabu.confirmOrder(
        sessionToken.authHeader, orderId, confirmBody
    ).wrapErrorMessage()

    fun transferFunds(
        sessionToken: NabuSessionTokenResponse,
        request: TransferRequest
    ): Single<String> = nabu.transferFunds(
        sessionToken.authHeader,
        request
    ).map {
        when (it.code()) {
            200 -> it.body()?.id.orEmpty()
            403 -> if (it.body()?.code == TransferFundsResponse.ERROR_WITHDRAWL_LOCKED)
                throw TransactionError.WithdrawalBalanceLocked
            else
                throw TransactionError.WithdrawalAlreadyPending
            409 -> throw TransactionError.WithdrawalInsufficientFunds
            else -> throw TransactionError.UnexpectedError
        }
    }.wrapErrorMessage()

    fun paymentMethods(
        sessionToken: NabuSessionTokenResponse,
        currency: String,
        eligibleOnly: Boolean,
        tier: Int? = null
    ) = nabu.getPaymentMethodsForSimpleBuy(
        authorization = sessionToken.authHeader,
        currency = currency,
        tier = tier,
        eligibleOnly = eligibleOnly
    ).wrapErrorMessage()

    fun getBeneficiaries(sessionToken: NabuSessionTokenResponse) =
        nabu.getLinkedBeneficiaries(sessionToken.authHeader).wrapErrorMessage()

    fun linkToABank(
        sessionToken: NabuSessionTokenResponse,
        fiatCurrency: String
    ) = nabu.linkABank(
        sessionToken.authHeader,
        CreateLinkBankRequestBody(
            fiatCurrency
        )
    ).wrapErrorMessage()

    fun updateAccountProviderId(
        sessionToken: NabuSessionTokenResponse,
        id: String,
        body: UpdateProviderAccountBody
    ) = nabu.updateProviderAccount(
        sessionToken.authHeader,
        id,
        body
    ).wrapErrorMessage()

    fun getCards(
        sessionToken: NabuSessionTokenResponse
    ) = nabu.getCards(
        authorization = sessionToken.authHeader
    ).wrapErrorMessage()

    /**
     * If there is no rate for a given asset, this endpoint returns a 204, which must be parsed
     */
    fun getInterestRates(
        sessionToken: NabuSessionTokenResponse,
        currency: String
    ) = nabu.getInterestRates(authorization = sessionToken.authHeader, currency = currency)
        .flatMapMaybe {
            when (it.code()) {
                200 -> Maybe.just(it.body())
                204 -> Maybe.empty()
                else -> Maybe.error(HttpException(it))
            }
        }
        .wrapErrorMessage()

    fun getInterestAccountBalance(
        sessionToken: NabuSessionTokenResponse,
        currency: String
    ) = nabu.getInterestAccountDetails(
        authorization = sessionToken.authHeader,
        cryptoSymbol = currency
    ).flatMapMaybe {
        when (it.code()) {
            200 -> Maybe.just(it.body())
            204 -> Maybe.empty()
            else -> Maybe.error(HttpException(it))
        }
    }.wrapErrorMessage()

    fun getInterestAddress(
        sessionToken: NabuSessionTokenResponse,
        currency: String
    ) = nabu.getInterestAddress(authorization = sessionToken.authHeader, currency = currency)
        .wrapErrorMessage()

    fun getInterestActivity(
        sessionToken: NabuSessionTokenResponse,
        currency: String
    ) = nabu.getInterestActivity(authorization = sessionToken.authHeader, product = "savings", currency = currency)
        .wrapErrorMessage()

    fun getInterestLimits(
        sessionToken: NabuSessionTokenResponse,
        currency: String
    ) = nabu.getInterestLimits(authorization = sessionToken.authHeader, currency = currency)
        .wrapErrorMessage()

    fun createInterestWithdrawal(
        sessionToken: NabuSessionTokenResponse,
        body: InterestWithdrawalBody
    ) = nabu.createInterestWithdrawal(authorization = sessionToken.authHeader, body = body)
        .wrapErrorMessage()

    fun getInterestEnabled(
        sessionToken: NabuSessionTokenResponse
    ) = nabu.getInterestEnabled(authorization = sessionToken.authHeader)
        .wrapErrorMessage()

    fun getLinkedBank(
        sessionToken: NabuSessionTokenResponse,
        id: String
    ) = nabu.getLinkedBank(authorization = sessionToken.authHeader, id = id)
        .wrapErrorMessage()

    fun getBanks(
        sessionToken: NabuSessionTokenResponse
    ) = nabu.getBanks(authorization = sessionToken.authHeader)
        .wrapErrorMessage()

    fun startBankTransferPayment(
        sessionToken: NabuSessionTokenResponse,
        id: String,
        body: BankTransferPaymentBody
    ) = nabu.startBankTransferPayment(
        authorization = sessionToken.authHeader,
        id = id,
        body = body
    ).wrapErrorMessage()

    fun getBankTransferCharge(
        sessionToken: NabuSessionTokenResponse,
        paymentId: String
    ) = nabu.getBankTransferCharge(
        authorization = sessionToken.authHeader,
        paymentId = paymentId
    ).wrapErrorMessage()

    fun updateOpenBankingToken(
        url: String,
        sessionToken: NabuSessionTokenResponse,
        body: OpenBankingTokenBody
    ) = nabu.updateOpenBankingToken(
        url = url,
        authorization = sessionToken.authHeader,
        body = body
    ).wrapErrorMessage()

    fun executeTransfer(
        sessionToken: NabuSessionTokenResponse,
        body: ProductTransferRequestBody
    ) = nabu.executeTransfer(
        authorization = sessionToken.authHeader,
        body = body
    ).wrapErrorMessage()

    fun getRecurringBuyEligibility(
        sessionToken: NabuSessionTokenResponse
    ) = nabu.getRecurringBuyEligibility(
        authorization = sessionToken.authHeader
    ).wrapErrorMessage()

    fun getRecurringBuysForAsset(
        sessionToken: NabuSessionTokenResponse,
        assetTicker: String
    ) = nabu.getRecurringBuysForAsset(
        authorization = sessionToken.authHeader,
        assetTicker = assetTicker
    ).wrapErrorMessage()

    fun getRecurringBuyForId(
        sessionToken: NabuSessionTokenResponse,
        recurringBuyId: String
    ) = nabu.getRecurringBuyById(
        authorization = sessionToken.authHeader,
        recurringBuyId = recurringBuyId
    ).wrapErrorMessage()

    fun cancelRecurringBuy(
        sessionToken: NabuSessionTokenResponse,
        id: String
    ) = nabu.cancelRecurringBuy(
        authorization = sessionToken.authHeader,
        id = id
    ).wrapErrorMessage()

    companion object {
        internal const val CLIENT_TYPE = "APP"
    }
}