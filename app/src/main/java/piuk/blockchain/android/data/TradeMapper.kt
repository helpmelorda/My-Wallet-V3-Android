package piuk.blockchain.android.data

import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.api.trade.data.NextPaymentRecurringBuy
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import com.blockchain.nabu.models.responses.simplebuy.toRecurringBuyFrequency

class GetAccumulatedInPeriodToIsFirstTimeBuyerMapper : Mapper<List<AccumulatedInPeriod>, Boolean> {
    override fun map(type: List<AccumulatedInPeriod>): Boolean =
        type.first { it.termType == AccumulatedInPeriod.ALL }.amount.value.toLong() == 0L
}

class GetNextPaymentDateListToFrequencyDateMapper :
    Mapper<List<NextPaymentRecurringBuy>, List<EligibleAndNextPaymentRecurringBuy>> {
    override fun map(type: List<NextPaymentRecurringBuy>): List<EligibleAndNextPaymentRecurringBuy> {
        return type.map {
            EligibleAndNextPaymentRecurringBuy(
                period = it.period.toRecurringBuyFrequency(),
                nextPaymentDate = it.nextPayment,
                eligibleMethods = mapStringToPaymentMethod(it.eligibleMethods)
            )
        }.toList()
    }

    private fun mapStringToPaymentMethod(eligibleMethods: List<String>): List<PaymentMethodType> {
        return eligibleMethods.map {
            when (it) {
                NextPaymentRecurringBuy.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
                NextPaymentRecurringBuy.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
                NextPaymentRecurringBuy.FUNDS -> PaymentMethodType.FUNDS
                else -> PaymentMethodType.UNKNOWN
            }
        }.toList()
    }
}