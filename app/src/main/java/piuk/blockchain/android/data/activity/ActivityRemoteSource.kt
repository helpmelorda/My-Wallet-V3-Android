//package piuk.blockchain.android.data.activity
//
//import io.reactivex.rxjava3.core.Observable
//import piuk.blockchain.android.coincore.ActivitySummaryList
//import piuk.blockchain.android.coincore.Coincore
//
//class ActivityRemoteSource(private val coincore: Coincore) {
//    fun get(): Observable<ActivitySummaryList> {
//        return coincore.allWallets()
//            .flatMap { it.activity }
//            .map { list ->
//                list
//            }.toObservable()
//    }
//}