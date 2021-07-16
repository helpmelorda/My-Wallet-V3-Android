package piuk.blockchain.android.ui.recurringbuy.data

import com.blockchain.api.trade.data.AccumulatedInPeriod

interface TradeMapper<in A, out B> {
    fun map(type: A): B
}

object GetAccumulatedInPeriodToIsFirstTimeBuyerMapper : TradeMapper<List<AccumulatedInPeriod>, Boolean> {
    override fun map(type: List<AccumulatedInPeriod>): Boolean =
        type.first { it.termType == AccumulatedInPeriod.ALL }.amount.value.toLong() == 0L
}
