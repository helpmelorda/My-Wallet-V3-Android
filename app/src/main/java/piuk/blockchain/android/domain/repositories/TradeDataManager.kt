package piuk.blockchain.android.domain.repositories

import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import io.reactivex.rxjava3.core.Single

interface TradeDataManager {

    fun isFirstTimeBuyer(): Single<Boolean>

    fun getEligibilityAndNextPaymentDate(): Single<List<EligibleAndNextPaymentRecurringBuy>>
}