package com.blockchain.biometrics

/**
 * Represents biometric capabilities of the hardware device.
 */
interface BiometricAuth {
    /**
     * Is biometric auth enabled at the OS level
     */
    val isBiometricAuthEnabled: Boolean

    /**
     * Is biometric auth available on the device
     */
    val isHardwareDetected: Boolean

    /**
     * Are biometrics encrolled with the OS
     */
    val areBiometricsEnrolled: Boolean

    /**
     * Is biometric unlock enabled by user and is data valid
     */
    val isBiometricUnlockEnabled: Boolean

    /**
     * Disabled biometric auth for this user
     */
    fun setBiometricUnlockDisabled()
}

enum class BiometricsType {
    TYPE_REGISTER,
    TYPE_LOGIN
}
