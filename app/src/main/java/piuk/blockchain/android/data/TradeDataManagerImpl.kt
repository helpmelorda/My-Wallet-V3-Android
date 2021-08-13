package piuk.blockchain.android.data

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.api.trade.data.NextPaymentRecurringBuy
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataManager
import java.time.ZonedDateTime

class TradeDataManagerImpl(
    private val tradeService: TradeService,
    private val authenticator: Authenticator,
    private val accumulatedInPeriodMapper: Mapper<List<AccumulatedInPeriod>, Boolean>,
    private val nextPaymentDateMapper:
    Mapper<List<NextPaymentRecurringBuy>, Map<RecurringBuyFrequency, ZonedDateTime>>
) : TradeDataManager {

    override fun isFirstTimeBuyer(): Single<Boolean> {
        return authenticator.authenticate { tokenResponse ->
            tradeService.isFirstTimeBuyer(authHeader = tokenResponse.authHeader)
                .map {
                    accumulatedInPeriodMapper.map(it.tradesAccumulated)
                }
        }
    }

    override fun getNextPaymentDate(): Single<Map<RecurringBuyFrequency, ZonedDateTime>> {
        return authenticator.authenticate { tokenResponse ->
            tradeService.getNextPaymentDate(authHeader = tokenResponse.authHeader)
                .map {
                    nextPaymentDateMapper.map(it.nextPayments)
                }
        }
    }
}