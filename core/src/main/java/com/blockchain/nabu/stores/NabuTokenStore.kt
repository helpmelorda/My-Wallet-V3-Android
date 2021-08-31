package com.blockchain.nabu.stores

import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.utils.Optional
import io.reactivex.rxjava3.core.Observable

interface NabuTokenStore {

    fun getAccessToken(): Observable<Optional<NabuSessionTokenResponse>>
}