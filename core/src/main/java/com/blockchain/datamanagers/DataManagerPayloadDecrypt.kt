package com.blockchain.datamanagers

import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.payload.PayloadDecrypt
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class DataManagerPayloadDecrypt(
    private val payloadDataManager: PayloadDataManager,
    private val bchDataManager: BchDataManager
) : PayloadDecrypt {

    override val isDoubleEncrypted: Boolean
        get() = payloadDataManager.isDoubleEncrypted

    override fun decryptHDWallet(validatedSecondPassword: String) =
        payloadDataManager.decryptHDWallet(validatedSecondPassword)

    override fun decryptWatchOnlyWallet() =
        bchDataManager.decryptWatchOnlyWallet(payloadDataManager.mnemonic)
}
