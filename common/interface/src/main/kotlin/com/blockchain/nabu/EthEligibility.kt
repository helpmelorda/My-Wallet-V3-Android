package com.blockchain.nabu

import io.reactivex.rxjava3.core.Single

interface EthEligibility {
    fun isEligible(): Single<Boolean>
}