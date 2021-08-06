package piuk.blockchain.androidcore.data.datastores.persistentstore

import io.reactivex.rxjava3.core.Observable

abstract class FetchStrategy<T> {

    abstract fun fetch(): Observable<T>
}