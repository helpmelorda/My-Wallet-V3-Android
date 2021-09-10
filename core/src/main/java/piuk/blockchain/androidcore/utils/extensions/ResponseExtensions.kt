package piuk.blockchain.androidcore.utils.extensions

import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response

fun Response<ResponseBody>.handleResponse(): Single<JsonObject> =
    if (isSuccessful) {
        body()?.let { responseBody ->
            Single.just(Json.parseToJsonElement(responseBody.string()) as JsonObject)
        } ?: Single.error(UnknownErrorException())
    } else {
        val errorResponse = errorBody()?.string()

        errorResponse?.let {
            if (it.contains(ACCOUNT_LOCKED)) {
                Single.error(AccountLockedException())
            } else {
                val errorBody = Json.parseToJsonElement(it) as JsonObject
                Single.error(
                    when {
                        errorBody.containsKey(INITIAL_ERROR) -> InitialErrorException()
                        errorBody.containsKey(KEY_AUTH_REQUIRED) -> AuthRequiredException()
                        else -> UnknownErrorException()
                    }
                )
            }
        } ?: kotlin.run {
            Single.error(UnknownErrorException())
        }
    }

private const val INITIAL_ERROR = "initial_error"
private const val KEY_AUTH_REQUIRED = "authorization_required"
private const val ACCOUNT_LOCKED = "locked"

class AuthRequiredException : Exception()
class InitialErrorException : Exception()
class AccountLockedException : Exception()
class UnknownErrorException : Exception()