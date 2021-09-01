package piuk.blockchain.android.data

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.api.trade.data.NextPaymentRecurringBuy
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataManager

class TradeDataManagerImpl(
    private val tradeService: TradeService,
    private val authenticator: Authenticator,
    private val accumulatedInPeriodMapper: Mapper<List<AccumulatedInPeriod>, Boolean>,
    private val nextPaymentRecurringBuyMapper:
    Mapper<List<NextPaymentRecurringBuy>, List<EligibleAndNextPaymentRecurringBuy>>
) : TradeDataManager {

    override fun isFirstTimeBuyer(): Single<Boolean> {
        return authenticator.authenticate { tokenResponse ->
            tradeService.isFirstTimeBuyer(authHeader = tokenResponse.authHeader)
                .map {
                    accumulatedInPeriodMapper.map(it.tradesAccumulated)
                }
        }
    }

    override fun getEligibilityAndNextPaymentDate(): Single<List<EligibleAndNextPaymentRecurringBuy>> {
        return authenticator.authenticate { tokenResponse ->
            tradeService.getNextPaymentDate(authHeader = tokenResponse.authHeader)
                .map {
                    nextPaymentRecurringBuyMapper.map(it.nextPayments)
                }
        }
    }
}