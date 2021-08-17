package piuk.blockchain.android.data.historicRate

import com.blockchain.core.price.ExchangeRate
import com.squareup.sqldelight.runtime.rx3.asObservable
import com.squareup.sqldelight.runtime.rx3.mapToOne
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.Database

class HistoricRateLocalSource(private val database: Database) {
    fun get(selectedFiat: String, asset: AssetInfo, requestedTimestamp: Long): Single<ExchangeRate> {
        return database.historicRateQueries.selectByKeys(asset.ticker, selectedFiat, requestedTimestamp).asObservable()
            .mapToOne().map {
            ExchangeRate.CryptoToFiat(
                from = asset,
                to = selectedFiat,
                rate = it.price.toBigDecimal()
            ) as ExchangeRate
        }.firstOrError()
    }

    fun insert(selectedFiat: String, asset: AssetInfo, requestedTimestamp: Long, price: Double) {
        database.historicRateQueries.insert(asset.ticker, selectedFiat, price, requestedTimestamp)
    }
}