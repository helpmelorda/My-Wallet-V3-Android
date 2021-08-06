package com.blockchain.api.services

import com.blockchain.api.wallet.WalletApiInterface
import com.blockchain.api.wallet.data.WalletSettingsDto
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class WalletSettingsService internal constructor(
    private val api: WalletApiInterface,
    private val apiCode: String
) {
    fun fetchWalletSettings(
        guid: String,
        sharedKey: String
    ): Single<WalletSettingsDto> =
        api.fetchSettings(guid = guid, sharedKey = sharedKey, apiCode = apiCode)

    fun triggerAlert(
        guid: String,
        sharedKey: String
    ): Completable = api.triggerAlert(guid = guid, sharedKey = sharedKey)
}