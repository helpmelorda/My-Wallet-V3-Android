package piuk.blockchain.androidcore.utils

import android.annotation.SuppressLint
import com.blockchain.common.util.AndroidDeviceIdGenerator
import com.blockchain.common.util.AndroidDeviceIdSource
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent

interface DeviceIdGenerator {
    fun generateId(): String
}

internal class DeviceIdGeneratorImpl(
    private val platformDeviceIdGenerator: AndroidDeviceIdGenerator,
    private val analytics: Analytics
) : DeviceIdGenerator {

    @SuppressLint("HardwareIds")
    override fun generateId(): String {
        val result = platformDeviceIdGenerator.generateId()

        val analyticsEvent = when (result.deviceIdSource) {
            AndroidDeviceIdSource.Uuid -> SOURCE_UUID_GEN
            AndroidDeviceIdSource.MacAddress -> SOURCE_MAC_ADDRESS
            AndroidDeviceIdSource.AndroidId -> SOURCE_ANDROID_ID
        }
        analytics.logEvent(AnalyticsGenEvent(analyticsEvent))

        return result.deviceId
    }

    private class AnalyticsGenEvent(val source: String) : AnalyticsEvent {
        override val event: String
            get() = EVENT_NAME

        override val params: Map<String, String>
            get() = mapOf(ANALYTICS_PARAM to source)
    }

    companion object {
        const val EVENT_NAME = "generateId"
        const val ANALYTICS_PARAM = "source"
        const val SOURCE_ANDROID_ID = "android_id"
        const val SOURCE_MAC_ADDRESS = "wifi_mac"
        const val SOURCE_UUID_GEN = "uuid_gen"
    }
}