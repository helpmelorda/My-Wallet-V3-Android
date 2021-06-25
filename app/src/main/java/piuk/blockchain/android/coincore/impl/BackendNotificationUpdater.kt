package piuk.blockchain.android.coincore.impl

import android.annotation.SuppressLint
import com.blockchain.preferences.AuthPrefs
import com.google.gson.Gson
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.data.api.NotificationReceiveAddresses
import timber.log.Timber
import java.lang.IllegalStateException

data class NotificationAddresses(
    val assetTicker: String,
    val addressList: List<String>
)

// Update the BE with the current address sets for assets, used to
// send notifications back to the app when Tx's complete
// Chains with the local offline cache, which supports swipe to receive
class BackendNotificationUpdater(
    private val walletApi: WalletApi,
    private val prefs: AuthPrefs
) {

    private val addressMap = mutableMapOf<String, NotificationAddresses>()

    @SuppressLint("CheckResult")
    @Synchronized
    fun updateNotificationBackend(item: NotificationAddresses) {
        addressMap[item.assetTicker] = item
        if (item.assetTicker in REQUIRED_ASSETS && requiredAssetsUpdated()) {
            // This is a fire and forget operation.
            // We don't want this call to delay the main rx chain, and we don't care about errors,
            updateBackend()
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { Timber.e("Notification Update failed: $it") })
        }
    }

    private fun requiredAssetsUpdated(): Boolean {
        REQUIRED_ASSETS.forEach { if (!addressMap.containsKey(it)) return@requiredAssetsUpdated false }
        return true
    }

    @Synchronized
    private fun updateBackend() =
        walletApi.submitCoinReceiveAddresses(
            prefs.walletGuid,
            prefs.sharedKey,
            coinReceiveAddresses()
        ).ignoreElements()

    private fun coinReceiveAddresses(): String =
        Gson().toJson(
            REQUIRED_ASSETS.map { key ->
                val addresses = addressMap[key]?.addressList ?: throw IllegalStateException("Required Asset missing")
                NotificationReceiveAddresses(key, addresses)
            }
        )

    companion object {
        private val REQUIRED_ASSETS = setOf(
            CryptoCurrency.BTC.ticker,
            CryptoCurrency.BCH.ticker,
            CryptoCurrency.ETHER.ticker
        )
    }
}
