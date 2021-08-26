package com.blockchain.biometrics

interface BiometricDataRepository {
    /**
     * Whether or not biometrics is enabled for this user, meaning they have enabled and successfully completed
     * biometric authorization.
     */
    fun isBiometricsEnabled(): Boolean

    /**
     * Toggle whether or not user has successfully enabled biometric authorization
     */
    fun setBiometricsEnabled(enabled: Boolean)

    /**
     * Store data encrypted with biometrics
     */
    fun storeBiometricEncryptedData(value: String)

    /**
     * Get data previously encrypted by biometrics
     */
    fun getBiometricEncryptedData() : String?

    /**
     * Clear data encrypted by biometrics
     */
    fun clearBiometricEncryptedData()
}