package piuk.blockchain.androidcore.data.api.interceptors

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import piuk.blockchain.androidcore.data.rxjava.SSLPinningEmitter
import javax.net.ssl.SSLPeerUnverifiedException

class SSLPinningInterceptor(val sslPinningEmitter: SSLPinningEmitter) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        try {
            return chain.proceed(request)
        } catch (exception: SSLPeerUnverifiedException) {
            sslPinningEmitter.emit()
        }

        // If an SSL exception was captured, we are returning a fake response
        // with a forbidden status code.
        //
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .message("")
            .code(403)
            .build()
    }
}