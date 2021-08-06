package piuk.blockchain.android.ui.recurringbuy.domain.usecases

import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.recurringbuy.domain.TradeRepository

class IsFirstTimeBuyerUseCase(
    private val tradeRepository: TradeRepository
) : UseCase<Unit, Single<Boolean>>() {

    override fun execute(parameter: Unit): Single<Boolean> =
            tradeRepository.isFirstTimeBuyer()
}