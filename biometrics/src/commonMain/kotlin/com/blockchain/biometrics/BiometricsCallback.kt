package com.blockchain.biometrics

/**
 * Callbacks from biometric authentication attempts
 */
interface BiometricsCallback<TBiometricData : BiometricData> {
    fun onAuthSuccess(data: TBiometricData)

    fun onAuthFailed(error: BiometricAuthError)

    fun onAuthCancelled()
}