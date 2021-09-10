package com.blockchain.preferences

interface AuthPrefs {
    var encodedPin: String
    var biometricsEnabled: Boolean

    fun clearEncodedPin()

    var sharedKey: String
    var walletGuid: String
    var encryptedPassword: String
    var pinFails: Int
    var sessionId: String

    var emailVerified: Boolean

    fun clearSessionId()
}