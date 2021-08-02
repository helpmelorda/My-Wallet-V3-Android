package piuk.blockchain.android.domain.repositories

import com.blockchain.api.nabu.data.GeolocationResponse
import io.reactivex.rxjava3.core.Single

interface NabuUserDataManager {

    fun getUserGeolocation(): Single<GeolocationResponse>
}