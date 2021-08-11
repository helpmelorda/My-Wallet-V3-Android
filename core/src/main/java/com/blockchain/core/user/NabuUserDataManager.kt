package com.blockchain.core.user

import com.blockchain.api.services.Geolocation
import com.blockchain.api.services.NabuUserService
import com.blockchain.auth.AuthHeaderProvider
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface NabuUserDataManager {

    fun getUserGeolocation(): Single<Geolocation>

    fun saveUserInitialLocation(countryIsoCode: String, stateIsoCode: String?): Completable
}

class NabuUserDataManagerImpl(
    private val nabuUserService: NabuUserService,
    private val authenticator: AuthHeaderProvider
) : NabuUserDataManager {

    override fun getUserGeolocation(): Single<Geolocation> =
        nabuUserService.getGeolocation()

    override fun saveUserInitialLocation(countryIsoCode: String, stateIsoCode: String?): Completable =
        authenticator.getAuthHeader().map {
            nabuUserService.saveUserInitialLocation(
                it,
                countryIsoCode,
                stateIsoCode
            )
        }.flatMapCompletable { it }
}