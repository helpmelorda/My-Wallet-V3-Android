package piuk.blockchain.androidcore.utils

import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.androidcore.data.api.ConnectionApi
import timber.log.Timber
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * Certificates to be pinned are derived via `openssl s_client -connect api.blockchain.info:443
 * | openssl x509 -pubkey -noout | openssl rsa -pubin -outform der | openssl dgst -sha256 -binary |
 * openssl enc -base64`, which returns a SHA-256 hash in Base64.
 */

class SSLVerifyUtil(private val connectionApi: ConnectionApi) {

    /**
     * Pings the Explorer to check for a connection. If the call returns an [ ] or
     * [SSLPeerUnverifiedException], the [ ] object will broadcast this to the BaseAuthActivity
     * which will handle the response appropriately.
     */
    fun validateSSL() =
        connectionApi.getExplorerConnection()
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onError = { Timber.e(it) }
            )
}