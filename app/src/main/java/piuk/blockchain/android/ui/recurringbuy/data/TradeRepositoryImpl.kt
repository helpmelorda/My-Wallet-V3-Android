package piuk.blockchain.android.ui.recurringbuy.data

import com.blockchain.api.services.TradeService
import com.blockchain.nabu.Authenticator
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.recurringbuy.domain.TradeRepository

class TradeRepositoryImpl(
    private val tradeService: TradeService,
    private val authenticator: Authenticator
) : TradeRepository {

    override fun isFirstTimeBuyer(): Single<Boolean> {
        return authenticator.authenticate { tokenResponse ->
            tradeService.isFirstTimeBuyer(authHeader = tokenResponse.authHeader).map {
                GetAccumulatedInPeriodToIsFirstTimeBuyerMapper.map(it)
            }
        }
    }
}