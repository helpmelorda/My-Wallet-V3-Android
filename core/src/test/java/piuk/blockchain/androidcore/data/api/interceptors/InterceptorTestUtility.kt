package piuk.blockchain.androidcore.data.api.interceptors

import com.nhaarman.mockitokotlin2.internal.createInstance
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.mockito.ArgumentMatcher
import org.mockito.Mockito

object InterceptorTestUtility {
    fun givenABasicRequest() = Request.Builder().url("http://www.blockchain.com").build()

    fun withAnyRequestMatching(request: Request): Request {
        return Mockito.argThat(RequestMatcher(request)) ?: createInstance()
    }

    private class RequestMatcher(val request: Request) : ArgumentMatcher<Request> {
        override fun matches(argument: Request?): Boolean {
            return if (argument == null) {
                false
            } else {
                request.method == argument.method &&
                    request.url == argument.url &&
                    request.headers == argument.headers &&
                    request.isHttps == argument.isHttps
            }
        }
    }
}

fun Request.someResponse(): Response = Response.Builder()
    .request(this)
    .protocol(Protocol.HTTP_1_1)
    .message("")
    .body(null)
    .code(200)
    .build()