package com.blockchain.preferences

interface AppInfoPrefs {
    var installationVersionName: String
    var currentStoredVersionCode: Int

    companion object {
        const val DEFAULT_APP_VERSION_CODE = -1
        const val DEFAULT_APP_VERSION_NAME = ""
    }
}