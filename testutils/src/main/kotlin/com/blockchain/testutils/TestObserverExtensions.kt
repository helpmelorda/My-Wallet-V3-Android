package com.blockchain.nabu.util

import io.reactivex.rxjava3.observers.TestObserver

fun <T> TestObserver<T>.waitForCompletionWithoutErrors(): TestObserver<T> {
    assertComplete()
    assertNoErrors()
    return this
}