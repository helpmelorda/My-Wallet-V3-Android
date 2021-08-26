package com.blockchain.biometrics

sealed class BiometricAuthError {
    object BiometricAuthLockout : BiometricAuthError()
    object BiometricAuthLockoutPermanent : BiometricAuthError()
    data class BiometricAuthOther(val error: String) : BiometricAuthError()
    object BiometricAuthFailed : BiometricAuthError()
    object BiometricKeysInvalidated : BiometricAuthError()
    object BiometricsNoSuitableMethods : BiometricAuthError()
}