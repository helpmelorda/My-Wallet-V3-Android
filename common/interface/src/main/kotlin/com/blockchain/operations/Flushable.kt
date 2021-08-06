package com.blockchain.operations

import io.reactivex.rxjava3.core.Completable

interface Flushable {
    fun flush(): Completable
}

interface AppStartUpFlushable : Flushable {
    val tag: String
}