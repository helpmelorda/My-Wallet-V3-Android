package com.blockchain.sunriver
import io.reactivex.rxjava3.core.Single

interface XlmHorizonUrlFetcher {
    fun xlmHorizonUrl(def: String): Single<String>
}