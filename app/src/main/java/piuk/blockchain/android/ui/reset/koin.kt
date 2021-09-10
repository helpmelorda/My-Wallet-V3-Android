package piuk.blockchain.android.ui.reset

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
import piuk.blockchain.android.ui.reset.password.ResetPasswordInteractor
import piuk.blockchain.android.ui.reset.password.ResetPasswordModel
import piuk.blockchain.android.ui.reset.password.ResetPasswordState

val resetAccountModule = module {

    scope(payloadScopeQualifier) {

        factory {
            ResetAccountModel(
                initialState = ResetAccountState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            ResetPasswordModel(
                initialState = ResetPasswordState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                interactor = get()
            )
        }

        factory {
            ResetPasswordInteractor(
                authDataManager = get(),
                payloadDataManager = get(),
                accessState = get(),
                prefs = get(),
                nabuDataManager = get(),
                metadataManager = get(),
                metadataRepository = get(),
                prngFixer = get(),
                walletPrefs = get()
            )
        }
    }
}