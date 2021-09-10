package piuk.blockchain.android.data.biometrics

import com.blockchain.biometrics.BiometricDataRepository
import piuk.blockchain.androidcore.utils.PrefsUtil

class BiometricsDataRepositoryImpl(val prefsUtil: PrefsUtil) : BiometricDataRepository {
    override fun isBiometricsEnabled(): Boolean =
        prefsUtil.biometricsEnabled

    override fun setBiometricsEnabled(enabled: Boolean) {
        prefsUtil.biometricsEnabled = enabled
    }

    override fun storeBiometricEncryptedData(value: String) {
        prefsUtil.encodedPin = value
    }

    override fun getBiometricEncryptedData(): String? =
        prefsUtil.encodedPin

    override fun clearBiometricEncryptedData() {
        prefsUtil.clearEncodedPin()
    }
}