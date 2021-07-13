package com.blockchain.nabu

import io.reactivex.rxjava3.core.Single

interface CurrentTier {

    fun usersCurrentTier(): Single<Int>
}