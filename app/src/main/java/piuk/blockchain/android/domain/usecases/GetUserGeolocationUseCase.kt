package piuk.blockchain.android.domain.usecases

import com.blockchain.api.nabu.data.GeolocationResponse
import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.NabuUserDataManager

class GetUserGeolocationUseCase(
    private val nabuUserDataManager: NabuUserDataManager
) : UseCase<Unit, Single<GeolocationResponse>>() {

    override fun execute(parameter: Unit): Single<GeolocationResponse> =
        nabuUserDataManager.getUserGeolocation()
}