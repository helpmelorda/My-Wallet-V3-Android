package com.blockchain.sunriver

import com.blockchain.fees.FeeType
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Single

interface XlmFeesFetcher {
    fun operationFee(feeType: FeeType): Single<CryptoValue>
}
