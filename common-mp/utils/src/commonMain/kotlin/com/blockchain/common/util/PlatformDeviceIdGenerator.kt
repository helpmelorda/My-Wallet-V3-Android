package com.blockchain.common.util

interface PlatformDeviceIdGenerator<PlatformIdSource : DeviceIdSource> {
    fun generateId(): PlatformDeviceId<PlatformIdSource>
}

interface DeviceIdSource

data class PlatformDeviceId<Type : DeviceIdSource>(val deviceId: String, val deviceIdSource: Type)