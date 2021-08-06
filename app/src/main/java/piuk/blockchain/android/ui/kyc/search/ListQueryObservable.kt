package piuk.blockchain.android.ui.kyc.search

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.BiFunction

class ListQueryObservable<T>(
    private val queryObservable: Observable<CharSequence>,
    private val listObservable: Observable<List<T>>
) {

    fun matchingItems(
        filter: (CharSequence, List<T>) -> List<T>
    ): Observable<List<T>> =
        Observable.combineLatest(
            listObservable,
            queryObservable,
            BiFunction { list, input -> filter(input, list) }
        )
}
