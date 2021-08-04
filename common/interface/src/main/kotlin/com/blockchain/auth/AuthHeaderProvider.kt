package com.blockchain.auth

import io.reactivex.rxjava3.core.Single

interface AuthHeaderProvider {
    fun getAuthHeader(): Single<String>
}