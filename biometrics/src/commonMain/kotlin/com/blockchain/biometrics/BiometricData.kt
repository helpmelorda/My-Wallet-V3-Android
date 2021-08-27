package com.blockchain.biometrics

/**
 * Represents a group of data that should be encrypted by [AndroidBiometrics].
 */
interface BiometricData {
    fun asByteArray(): ByteArray
}

interface BiometricDataFactory<TBiometricData : BiometricData> {
    fun fromByteArray(byteArray: ByteArray): TBiometricData
}