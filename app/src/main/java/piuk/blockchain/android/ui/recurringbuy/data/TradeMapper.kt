package piuk.blockchain.android.ui.recurringbuy.data

import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.api.trade.data.NextPaymentRecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.responses.simplebuy.toRecurringBuyFrequency
import java.time.ZonedDateTime

interface TradeMapper<in A, out B> {
    fun map(type: A): B
}

class GetAccumulatedInPeriodToIsFirstTimeBuyerMapper : TradeMapper<List<AccumulatedInPeriod>, Boolean> {
    override fun map(type: List<AccumulatedInPeriod>): Boolean =
        type.first { it.termType == AccumulatedInPeriod.ALL }.amount.value.toLong() == 0L
}

class GetNextPaymentDateListToFrequencyDateMapper :
    TradeMapper<List<NextPaymentRecurringBuy>, Map<RecurringBuyFrequency, ZonedDateTime>> {
    override fun map(type: List<NextPaymentRecurringBuy>): Map<RecurringBuyFrequency, ZonedDateTime> {
        return type.associate { it.period.toRecurringBuyFrequency() to ZonedDateTime.parse(it.nextPayment) }
    }
}
