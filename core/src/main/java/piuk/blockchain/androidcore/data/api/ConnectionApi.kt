package piuk.blockchain.androidcore.data.api

import io.reactivex.rxjava3.core.Observable
import okhttp3.ResponseBody
import retrofit2.Retrofit

class ConnectionApi(retrofit: Retrofit) {

    private val connectionEndpoint: ConnectionEndpoint =
        retrofit.create(ConnectionEndpoint::class.java)

    fun getExplorerConnection(): Observable<ResponseBody> =
        connectionEndpoint.pingExplorer()
}
