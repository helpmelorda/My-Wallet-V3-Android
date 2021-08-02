package piuk.blockchain.android.domain.usecases

import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataManager
import java.time.ZonedDateTime

class GetNextPaymentDateUseCase(
    private val tradeDataManager: TradeDataManager
) : UseCase<Unit, Single<Map<RecurringBuyFrequency, ZonedDateTime>>>() {

    override fun execute(parameter: Unit): Single<Map<RecurringBuyFrequency, ZonedDateTime>> =
        tradeDataManager.getNextPaymentDate()
}