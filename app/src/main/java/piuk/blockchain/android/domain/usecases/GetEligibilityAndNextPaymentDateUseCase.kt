package piuk.blockchain.android.domain.usecases

import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataManager

class GetEligibilityAndNextPaymentDateUseCase(
    private val tradeDataManager: TradeDataManager
) : UseCase<Unit, Single<List<EligibleAndNextPaymentRecurringBuy>>>() {

    override fun execute(parameter: Unit): Single<List<EligibleAndNextPaymentRecurringBuy>> =
        tradeDataManager.getEligibilityAndNextPaymentDate()
}