package com.blockchain.nabu.datamanagers.repositories

import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.responses.cards.PaymentMethodResponse
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Single

interface RecurringBuyEligibilityProvider {
    fun getRecurringBuyEligibility(): Single<List<PaymentMethodType>>
}

class RecurringBuyEligibilityProviderImpl(
    private val nabuService: NabuService,
    private val authenticator: Authenticator,
    private val features: InternalFeatureFlagApi
) : RecurringBuyEligibilityProvider {
    override fun getRecurringBuyEligibility(): Single<List<PaymentMethodType>> =
        if (features.isFeatureEnabled(GatedFeature.RECURRING_BUYS)) {
            authenticator.authenticate { sessionToken ->
                nabuService.getRecurringBuyEligibility(sessionToken).map {
                    it.eligibleMethods.map { method ->
                        when (method) {
                            PaymentMethodResponse.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
                            PaymentMethodResponse.FUNDS -> PaymentMethodType.FUNDS
                            PaymentMethodResponse.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
                            PaymentMethodResponse.BANK_ACCOUNT -> PaymentMethodType.BANK_ACCOUNT
                            else -> PaymentMethodType.UNKNOWN
                        }
                    }
                }
            }
        } else {
            Single.just(emptyList())
        }
    }

class RecurringBuyRepository(
    private val recurringBuyEligibilityProvider: RecurringBuyEligibilityProvider
) {
    fun getRecurringBuyEligibleMethods() = recurringBuyEligibilityProvider.getRecurringBuyEligibility()
}