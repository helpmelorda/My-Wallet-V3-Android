package piuk.blockchain.android.data.biometrics

import com.blockchain.biometrics.BiometricData
import com.blockchain.biometrics.BiometricDataFactory
import java.nio.charset.Charset

class WalletBiometricData(val accessPin: String) : BiometricData {
    override fun asByteArray(): ByteArray =
        accessPin.toByteArray(Charset.forName("UTF-8"))
}

class WalletBiometricDataFactory : BiometricDataFactory<WalletBiometricData> {
    override fun fromByteArray(byteArray: ByteArray): WalletBiometricData =
        WalletBiometricData(String(byteArray, Charset.forName("UTF-8")))
}