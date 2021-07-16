package piuk.blockchain.androidcore.data.api.interceptors

import okhttp3.Interceptor
import okhttp3.Response

class RequestIdInterceptor(val generator: (() -> String)) : Interceptor {
    companion object {
        private const val requestIDHeaderKey: String = "X-Request-ID"
    }

    /**
     *
     * Inserts a randomly created UUID value for the X-Request-ID header if there is none present.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        return if (originalRequest.headers[requestIDHeaderKey] == null) {
            val updatedRequest = originalRequest.newBuilder()
                .header(requestIDHeaderKey, generator())
                .build()
            chain.proceed(updatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}