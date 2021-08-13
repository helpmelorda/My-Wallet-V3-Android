package piuk.blockchain.android.data

import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.api.trade.data.NextPaymentRecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.responses.simplebuy.toRecurringBuyFrequency
import java.time.ZonedDateTime

class GetAccumulatedInPeriodToIsFirstTimeBuyerMapper : Mapper<List<AccumulatedInPeriod>, Boolean> {
    override fun map(type: List<AccumulatedInPeriod>): Boolean =
        type.first { it.termType == AccumulatedInPeriod.ALL }.amount.value.toLong() == 0L
}

class GetNextPaymentDateListToFrequencyDateMapper :
    Mapper<List<NextPaymentRecurringBuy>, Map<RecurringBuyFrequency, ZonedDateTime>> {
    override fun map(type: List<NextPaymentRecurringBuy>): Map<RecurringBuyFrequency, ZonedDateTime> {
        return type.associate { it.period.toRecurringBuyFrequency() to ZonedDateTime.parse(it.nextPayment) }
    }
}
