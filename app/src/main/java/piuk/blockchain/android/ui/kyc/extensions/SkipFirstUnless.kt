package piuk.blockchain.android.ui.kyc.extensions

import io.reactivex.rxjava3.core.Observable

fun <T> Observable<T>.skipFirstUnless(predicate: (T) -> Boolean): Observable<T> =
    this.publish { upstream ->
        Observable.merge(
            upstream.take(1).filter(predicate),
            upstream.skip(1)
        )
    }