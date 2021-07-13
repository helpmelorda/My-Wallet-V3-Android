package com.blockchain.data.datastores

import io.reactivex.rxjava3.core.Observable

interface PersistentStore<T> {

    fun store(data: T): Observable<T>

    fun invalidate()
}