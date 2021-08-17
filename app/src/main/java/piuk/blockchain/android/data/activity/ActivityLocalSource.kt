package piuk.blockchain.android.data.activity

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.TransactionState
import com.squareup.sqldelight.runtime.rx3.asObservable
import com.squareup.sqldelight.runtime.rx3.mapToList
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Observable
import piuk.blockchain.android.Database
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.FiatActivitySummaryItem
import piuk.blockchain.android.coincore.fiat.FiatCustodialAccount

class ActivityLocalSource(private val database: Database, private val exchangeRates: ExchangeRatesDataManager, private val coincore: Coincore) {
    fun get(): Observable<ActivitySummaryList> {
        return database.transactionQueries.selectAll().asObservable().mapToList().map {
            val activitySummaryList = mutableListOf<ActivitySummaryItem>()
            val accountGroup = coincore.allWallets(true).blockingGet()
            val fiatAccount = accountGroup.accounts.find {
                it is FiatCustodialAccount
            }

            it.forEach { transaction ->
                if (transaction.transactionType == TransactionType.FiatValue.name && fiatAccount as? FiatCustodialAccount != null) {
                    val activityItem = FiatActivitySummaryItem(
                        currency = transaction.currency,
                        exchangeRates = exchangeRates,
                        account = fiatAccount,
                        state = TransactionState.valueOf(transaction.state),
                        type = com.blockchain.nabu.datamanagers.TransactionType.valueOf(transaction.state),
                        timeStampMs = transaction.insertedAt.toLong(),
                        txId = transaction.id,
                        value = FiatValue(transaction.amountSymbol, transaction.amountMinor.toBigDecimal())
                    )
                    activitySummaryList.add(activityItem)
                }
            }
            activitySummaryList
        }
    }

    fun insert(list: ActivitySummaryList) {
        list.forEach {
            when (it) {
                is FiatActivitySummaryItem -> {
                    database.transactionQueries.insert(
                        id = it.txId,
                        state = it.state.name,
                        currency = it.currency,
                        amountSymbol = it.value.symbol,
                        amountMinor = it.value.toStringWithoutSymbol(),
                        insertedAt = it.timeStampMs.toString(),
                        type = it.type.name,
                        transactionType = TransactionType.FiatValue.name,
                        accountRef = null,
                        feeMinor = null,
                        txHash = null
                    )
                }
            }

        }
    }
}

enum class TransactionType {
    FiatValue
}