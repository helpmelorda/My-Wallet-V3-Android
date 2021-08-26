package com.blockchain.common.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import timber.log.Timber
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

class AndroidDeviceIdGenerator(
    private val ctx: Context
) : PlatformDeviceIdGenerator<AndroidDeviceIdSource> {

    @SuppressLint("HardwareIds")
    override fun generateId(): PlatformDeviceId<AndroidDeviceIdSource> {

        // In most cases, the android_id should do what we want. However, sometimes this has been
        // known to return null and some devices have a bug where it returns the emulator id.
        // In those cases, we'll generate an id based on the wifi interface. IF we can't find
        // a wifi interface then... um... all bets are off - without expanding the app
        // permission set, there's not so many options. So then we'll just generate a UUID and
        // have to live with it's failure to persist across app installs.
        // We'll also track these and see if it actually comes up, in which case we can re-think.

        var id = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        val source = if ((id == null) || (id == BUGGY_ANDROID_ID)) {
            id = generateWifiMacId()
            if (id == null) {
                id = UUID.randomUUID().toString()
                AndroidDeviceIdSource.Uuid
            } else {
                AndroidDeviceIdSource.MacAddress
            }
        } else {
            AndroidDeviceIdSource.AndroidId
        }
        return PlatformDeviceId(id, source)
    }

    private fun generateWifiMacId(): String? {
        try {
            val hwIf = NetworkInterface.getNetworkInterfaces()
                .toList()
                .firstOrNull { it.displayName == "wlan0" }

            if (hwIf != null && hwIf.hardwareAddress != null) {
                val md = MessageDigest.getInstance("SHA-1")
                md.update(hwIf.hardwareAddress)
                md.update(Build.MANUFACTURER.toByteArray())
                md.update(Build.MODEL.toByteArray())
                md.update(Build.DEVICE.toByteArray())
                return md.digest().toHex()
            }
        } catch (e: Throwable) {
            Timber.d("Unable to generate mac based device id")
        }
        return null
    }

    companion object {
        const val BUGGY_ANDROID_ID = "9774d56d682e549c"
    }
}

sealed class AndroidDeviceIdSource : DeviceIdSource {
    object Uuid : AndroidDeviceIdSource()
    object MacAddress : AndroidDeviceIdSource()
    object AndroidId : AndroidDeviceIdSource()
}