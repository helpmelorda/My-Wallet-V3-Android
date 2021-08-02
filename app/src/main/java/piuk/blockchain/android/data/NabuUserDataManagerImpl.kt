package piuk.blockchain.android.data

import com.blockchain.api.nabu.data.GeolocationResponse
import com.blockchain.api.services.NabuUserService
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.NabuUserDataManager

class NabuUserDataManagerImpl(
    private val nabuUserService: NabuUserService
) : NabuUserDataManager {

    override fun getUserGeolocation(): Single<GeolocationResponse> =
        nabuUserService.getGeolocation().map { it }
}