package piuk.blockchain.android.ui.recurringbuy.domain.usecases

import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.recurringbuy.domain.TradeDataManager

class IsFirstTimeBuyerUseCase(
    private val tradeDataManager: TradeDataManager
) : UseCase<Unit, Single<Boolean>>() {

    override fun execute(parameter: Unit): Single<Boolean> =
            tradeDataManager.isFirstTimeBuyer()
}