package com.blockchain.logging

import io.reactivex.rxjava3.core.Completable

interface LastTxUpdater {

    fun updateLastTxTime(): Completable
}