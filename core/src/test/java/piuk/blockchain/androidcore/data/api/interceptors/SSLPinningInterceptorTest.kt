package piuk.blockchain.androidcore.data.api.interceptors

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import okhttp3.Interceptor
import org.junit.Test
import piuk.blockchain.androidcore.data.rxjava.SSLPinningSubject
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.test.assertEquals

class SSLPinningInterceptorTest {
    private val sslPinningSubject: SSLPinningSubject = mock()
    private val subject = SSLPinningInterceptor(sslPinningSubject)

    @Test
    fun `will send event to bus when SSL exception happens`() {
        val initialRequest = InterceptorTestUtility.givenABasicRequest()

        val interceptorChain: Interceptor.Chain = mock {
            on { request() }.thenReturn(initialRequest)
            on {
                proceed(InterceptorTestUtility.withAnyRequestMatching(initialRequest))
            }.thenThrow(SSLPeerUnverifiedException::class.java)
        }

        subject.intercept(interceptorChain)

        verify(sslPinningSubject).emit()
    }

    @Test
    fun `will return expected response when there are no exceptions`() {
        val initialRequest = InterceptorTestUtility.givenABasicRequest()
        val expectedResponse = initialRequest.someResponse()

        val interceptorChain: Interceptor.Chain = mock {
            on { request() }.thenReturn(initialRequest)
            on {
                proceed(InterceptorTestUtility.withAnyRequestMatching(initialRequest))
            }.thenReturn(expectedResponse)
        }

        val returnedResponse = subject.intercept(interceptorChain)

        assertEquals(returnedResponse, expectedResponse)
        verifyZeroInteractions(sslPinningSubject)
    }
}