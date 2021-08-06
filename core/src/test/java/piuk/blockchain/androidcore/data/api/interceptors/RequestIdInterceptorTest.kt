package piuk.blockchain.androidcore.data.api.interceptors

import com.nhaarman.mockitokotlin2.internal.createInstance
import com.nhaarman.mockitokotlin2.mock
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.Mockito
import kotlin.test.assertEquals

class RequestIdInterceptorTest {
    private val generatedHeaderValue = "489a62fd-a936-473d-8687-8f702aefd30c"
    private val interceptor: RequestIdInterceptor = RequestIdInterceptor { generatedHeaderValue }

    @Test
    fun `Any intercepted request will contain an X-Request-ID header`() {
        val initialRequest = givenABasicRequest()
        val expectedRequestToBeTriggered = initialRequest.withRequestID(generatedHeaderValue)
        val response = expectedRequestToBeTriggered.someResponse()

        val interceptorChain: Interceptor.Chain = mock {
            on { request() }.thenReturn(initialRequest)
            on { proceed(withAnyRequestMatching(expectedRequestToBeTriggered)) }.thenReturn(response)
        }

        val returnedResponse = interceptor.intercept(interceptorChain)

        val headerValue = returnedResponse.request.header("X-Request-ID")
        assertEquals(generatedHeaderValue, headerValue)
    }

    @Test
    fun `Keep the X-Request-ID header for any intercepted request with it`() {
        val headerValueAlreadyPresent = "8b56ecaa-bd62-4a22-927f-015db47f6f49"
        val initialRequest = givenABasicRequest().withRequestID(headerValueAlreadyPresent)
        val response = initialRequest.someResponse()

        val interceptorChain: Interceptor.Chain = mock {
            on { request() }.thenReturn(initialRequest)
            on { proceed(withAnyRequestMatching(initialRequest)) }.thenReturn(response)
        }

        val returnedResponse = interceptor.intercept(interceptorChain)

        val headerValue = returnedResponse.request.header("X-Request-ID")
        assertEquals(headerValueAlreadyPresent, headerValue)
    }

    private fun givenABasicRequest() = Request.Builder().url("http://www.blockchain.com").build()
}

private fun Request.withRequestID(header: String): Request =
    newBuilder().addHeader("X-Request-ID", header).build()

private fun Request.someResponse(): Response = Response.Builder()
    .request(this)
    .protocol(Protocol.HTTP_1_1)
    .message("")
    .body(null)
    .code(200)
    .build()

private fun withAnyRequestMatching(request: Request): Request {
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