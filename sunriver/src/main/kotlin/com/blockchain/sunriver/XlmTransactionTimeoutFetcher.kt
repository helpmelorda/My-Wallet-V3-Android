package com.blockchain.sunriver

import io.reactivex.rxjava3.core.Single

interface XlmTransactionTimeoutFetcher {
    fun transactionTimeout(): Single<Long>
}
