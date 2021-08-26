package piuk.blockchain.android.data.biometrics

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.blockchain.biometrics.AndroidBiometricsController
import com.blockchain.biometrics.AndroidBiometricsControllerImpl
import com.blockchain.biometrics.BiometricAuth
import com.blockchain.biometrics.BiometricDataRepository
import com.blockchain.biometrics.BiometricsCallback
import com.blockchain.biometrics.BiometricsType
import com.blockchain.biometrics.CryptographyManager
import com.blockchain.biometrics.PromptInfo
import com.blockchain.logging.CrashLogger
import org.koin.core.component.KoinComponent
import piuk.blockchain.android.R
import java.util.concurrent.Executor

interface BiometricsController : BiometricAuth {
    fun authenticate(fragment: Fragment, type: BiometricsType, callback: BiometricsCallback<WalletBiometricData>)
    fun authenticate(
        activity: FragmentActivity,
        type: BiometricsType,
        callback: BiometricsCallback<WalletBiometricData>
    )
}

class BiometricsControllerImpl(
    private val applicationContext: Context,
    private val biometricData: WalletBiometricData,
    private val biometricDataFactory: WalletBiometricDataFactory,
    biometricDataRepository: BiometricDataRepository,
    biometricManager: BiometricManager,
    cryptographyManager: CryptographyManager,
    crashLogger: CrashLogger
) : BiometricsController, KoinComponent {

    private val androidBiometricsController: AndroidBiometricsController<WalletBiometricData> by lazy {
        AndroidBiometricsControllerImpl(
            biometricData,
            biometricDataFactory,
            biometricDataRepository,
            biometricManager,
            cryptographyManager,
            crashLogger
        )
    }

    override fun authenticate(
        fragment: Fragment,
        type: BiometricsType,
        callback: BiometricsCallback<WalletBiometricData>
    ) {
        androidBiometricsController.authenticate(getExecutor(), fragment, type, getPromptInfo(type), callback)
    }

    override fun authenticate(
        activity: FragmentActivity,
        type: BiometricsType,
        callback: BiometricsCallback<WalletBiometricData>
    ) {
        androidBiometricsController.authenticate(getExecutor(), activity, type, getPromptInfo(type), callback)
    }

    override val isBiometricAuthEnabled: Boolean
        get() = androidBiometricsController.isBiometricAuthEnabled

    override val isHardwareDetected: Boolean
        get() = androidBiometricsController.isHardwareDetected

    override val areBiometricsEnrolled: Boolean
        get() = androidBiometricsController.areBiometricsEnrolled

    override val isBiometricUnlockEnabled: Boolean
        get() = androidBiometricsController.isBiometricUnlockEnabled

    override fun setBiometricUnlockDisabled() {
        androidBiometricsController.setBiometricUnlockDisabled()
    }

    private fun getExecutor(): Executor = ContextCompat.getMainExecutor(applicationContext)

    private fun getPromptInfo(biometricsType: BiometricsType): PromptInfo =
        with(applicationContext) {
            if (biometricsType == BiometricsType.TYPE_REGISTER) {
                PromptInfo(
                    getString(R.string.fingerprint_login_title),
                    getString(R.string.fingerprint_register_description),
                    getString(R.string.common_cancel)
                )
            } else {
                PromptInfo(
                    getString(R.string.fingerprint_login_title),
                    getString(R.string.fingerprint_login_description),
                    getString(R.string.fingerprint_use_pin)
                )
            }
        }
}