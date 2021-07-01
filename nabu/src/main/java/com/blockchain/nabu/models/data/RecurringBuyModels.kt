package com.blockchain.nabu.models.data

import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatValue
import java.util.Date

enum class RecurringBuyState {
    ACTIVE,
    INACTIVE,
    UNINITIALISED
}

enum class RecurringBuyFrequency {
    ONE_TIME,
    DAILY,
    WEEKLY,
    BI_WEEKLY,
    MONTHLY,
    UNKNOWN
}

interface RecurringBuyPaymentDetails {
    val paymentDetails: PaymentMethodType
}

data class FundsAccount(val currency: String) : RecurringBuyPaymentDetails {
    override val paymentDetails: PaymentMethodType
        get() = PaymentMethodType.FUNDS
}

data class RecurringBuy(
    val id: String,
    val state: RecurringBuyState,
    val recurringBuyFrequency: RecurringBuyFrequency,
    val nextPaymentDate: Date,
    val paymentMethodType: PaymentMethodType,
    val paymentMethodId: String?,
    val amount: FiatValue,
    val asset: AssetInfo,
    val createDate: Date,
    val paymentDetails: RecurringBuyPaymentDetails? = null
)