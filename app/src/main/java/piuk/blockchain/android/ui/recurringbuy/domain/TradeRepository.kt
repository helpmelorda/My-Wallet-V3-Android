package piuk.blockchain.android.ui.recurringbuy.domain

import io.reactivex.rxjava3.core.Single

interface TradeRepository {

    fun isFirstTimeBuyer(): Single<Boolean>
}