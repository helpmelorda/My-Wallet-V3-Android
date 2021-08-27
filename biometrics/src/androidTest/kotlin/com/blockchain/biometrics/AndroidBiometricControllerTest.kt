package com.blockchain.biometrics

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import com.blockchain.logging.CrashLogger
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.Charset
import java.security.Signature
import javax.crypto.Cipher
import kotlin.test.assertEquals

// NOTE: roboelectric runner needed for Base64 Android implementations
@Suppress("PrivatePropertyName")
@Config(sdk = [24])
@RunWith(RobolectricTestRunner::class)
class BiometricsControllerTest {
    private lateinit var subject: AndroidBiometricsControllerImpl<TestBiometricData>

    private val biometricDataRepository: BiometricDataRepository = mock()
    private val biometricData = TestBiometricData(DATA_1)
    private val biometricDataFactory = TestBiometricDataFactory()
    private val cryptographyManager: CryptographyManager = mock()
    private val biometricManager: BiometricManager = mock()
    private val crashLogger: CrashLogger = mock()
    private val cipher = mock<Cipher>()
    private lateinit var cryptoObject: BiometricPrompt.CryptoObject

    @Before @Throws(java.lang.Exception::class)
    fun setup() {
        cryptoObject = BiometricPrompt.CryptoObject(cipher)
        // setup biometric manager to be enabled for device
        whenever(biometricManager.canAuthenticate(any())).thenReturn(BIOMETRIC_STRONG)
        @Suppress("DEPRECATION")
        whenever(biometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS)
        subject =
            AndroidBiometricsControllerImpl(
                biometricData, biometricDataFactory, biometricDataRepository, biometricManager, cryptographyManager,
                crashLogger
            )
    }

    @Test
    fun ifFingerprintUnlockEnabledTrue() {
        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn("1234")
        whenever(biometricDataRepository.isBiometricsEnabled()).thenReturn(true)

        val value = subject.isBiometricUnlockEnabled
        Assert.assertEquals(true, value)
    }

    @Test
    fun ifFingerprintUnlockEnabledFalse() {
        whenever(biometricDataRepository.isBiometricsEnabled()).thenReturn(false)

        val value = subject.isBiometricUnlockEnabled
        Assert.assertEquals(false, value)
    }

    @Test
    fun setFingerprintUnlockDisabled() {
        //        whenever(biometricDataRepository.encodedKeyName).thenReturn("a string")
        subject.setBiometricUnlockDisabled()
        verify(biometricDataRepository).setBiometricsEnabled(false)
        verify(biometricDataRepository).clearBiometricEncryptedData()
        verify(cryptographyManager).clearData(any())
    }

    @Test
    fun getDataAndIV_success() {
        val encodedValue = "ZGF0YQ==\n"
        val data = "$encodedValue-_-$encodedValue"
        val result = subject.getDataAndIV(data)
        Assert.assertEquals(result.first, encodedValue)
        Assert.assertEquals(result.second, encodedValue)
    }

    @Test(expected = IllegalStateException::class)
    fun getDataAndIV_noSeparator() {
        val encodedValue = "ZGF0YQ==\n"
        val data = "$encodedValue$encodedValue"
        subject.getDataAndIV(data)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getDataAndIV_noIV() {
        val encodedValue = "ZGF0YQ==\n"
        val data = "$encodedValue-_-"
        subject.getDataAndIV(data)
    }

    @Test
    fun authCallbackSuccess_register() {
        val encryptedData = EncryptedData(byteArrayOf(), byteArrayOf())
        val result = mock<BiometricPrompt.AuthenticationResult>()

        whenever(result.cryptoObject).thenReturn(cryptoObject)
        whenever(cryptographyManager.encryptData(any(), any())).thenReturn(encryptedData)

        val callback = mock<BiometricsCallback<TestBiometricData>>()
        val auth = subject.getAuthenticationCallback(callback, BiometricsType.TYPE_REGISTER)
        auth.onAuthenticationSucceeded(result)

        verify(callback).onAuthSuccess(any())
    }

    @Test
    fun authCallbackSuccess_login() {
        val result = mock<BiometricPrompt.AuthenticationResult>()

        val data = "1234-_-1234"
        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn(data)

        whenever(result.cryptoObject).thenReturn(cryptoObject)
        whenever(cryptographyManager.decryptData(any(), any())).thenReturn(
            DATA_1.toByteArray(Charset.forName("UTF-8"))
        )

        val callback = mock<BiometricsCallback<TestBiometricData>>()
        val auth = subject.getAuthenticationCallback(callback, BiometricsType.TYPE_LOGIN)
        auth.onAuthenticationSucceeded(result)

        assertEquals(biometricData.data, DATA_1)
        verify(callback).onAuthSuccess(any())
    }

    @Test
    fun authCallbackErrorLockout() {
        val callback = mock<BiometricsCallback<TestBiometricData>>()
        val auth = subject.getAuthenticationCallback(callback, BiometricsType.TYPE_REGISTER)

        auth.onAuthenticationError(BiometricPrompt.ERROR_LOCKOUT, "a string")
        verify(callback).onAuthFailed(BiometricAuthError.BiometricAuthLockout)
    }

    @Test
    fun authCallbackErrorLockoutPermanent() {
        val callback = mock<BiometricsCallback<TestBiometricData>>()
        val auth = subject.getAuthenticationCallback(callback, BiometricsType.TYPE_REGISTER)

        auth.onAuthenticationError(BiometricPrompt.ERROR_LOCKOUT_PERMANENT, "a string")
        verify(callback).onAuthFailed(BiometricAuthError.BiometricAuthLockoutPermanent)
    }

    @Test
    fun authCallbackErrorUnknown() {
        val callback = mock<BiometricsCallback<TestBiometricData>>()
        val auth = subject.getAuthenticationCallback(callback, BiometricsType.TYPE_REGISTER)

        val error = "error"
        auth.onAuthenticationError(-999, error)

        verify(callback).onAuthFailed(any())
    }

    @Test
    fun authCallbackFailure() {
        val callback = mock<BiometricsCallback<TestBiometricData>>()
        val auth = subject.getAuthenticationCallback(callback, BiometricsType.TYPE_REGISTER)

        auth.onAuthenticationFailed()
        verify(callback).onAuthFailed(BiometricAuthError.BiometricAuthFailed)
    }

    @Test
    fun authCallbackCancelledCta() {
        val callback = mock<BiometricsCallback<TestBiometricData>>()
        val auth = subject.getAuthenticationCallback(callback, BiometricsType.TYPE_REGISTER)

        val errString = "error"
        auth.onAuthenticationError(BiometricPrompt.ERROR_NEGATIVE_BUTTON, errString)
        verify(callback).onAuthCancelled()
    }

    @Test
    fun authCallbackCancelledCode() {
        val callback = mock<BiometricsCallback<TestBiometricData>>()
        val auth = subject.getAuthenticationCallback(callback, BiometricsType.TYPE_REGISTER)

        val errString = "error"
        auth.onAuthenticationError(BiometricPrompt.ERROR_USER_CANCELED, errString)
        verify(callback).onAuthCancelled()
    }

    @Test(expected = IllegalStateException::class)
    fun processDecryption_noData() {
        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn("")

        subject.processDecryption(cryptoObject)
    }

    @Test(expected = IllegalStateException::class)
    fun processDecryption_noCipher() {
        val data = "1234-_-1234" // "MTIzNC1fLTEyMzQ="
        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn(data)
        // cipher can only be null when the object is started with a different base param
        val cryptoObj = BiometricPrompt.CryptoObject(Signature.getInstance("SHA1withRSA"))
        subject.processDecryption(cryptoObj)
    }

    @Test
    fun processDecryption_success() {
        val data = "1234-_-1234" // "MTIzNC1fLTEyMzQ="
        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn(data)

        val pin = "1234"
        whenever(cryptographyManager.decryptData(any(), any())).thenReturn(pin.toByteArray(Charset.forName("UTF-8")))

        val decryptedData = String(subject.processDecryption(cryptoObject), Charset.forName("UTF-8"))
        Assert.assertEquals(pin, decryptedData)
    }

    @Test
    fun generateCompositeKey() {
        val byteArrayData = "data".toByteArray()
        val base64EncodedData = "ZGF0YQ==\n"
        val expectedValue = "$base64EncodedData-_-$base64EncodedData"

        val result = subject.generateCompositeKey(byteArrayData, byteArrayData)
        Assert.assertEquals(result, expectedValue)
    }

    @Test
    fun authForRegistration_success() {
        val biometricPrompt = mock<BiometricPrompt>()
        val promptInfo = mock<BiometricPrompt.PromptInfo>()
        val callback = mock<BiometricsCallback<TestBiometricData>>()

        val data = "1234-_-1234" // "MTIzNC1fLTEyMzQ="

        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn(data)

        val cipherSuccess = CipherState.CipherSuccess(mock())
        whenever(
            cryptographyManager.getInitializedCipherForEncryption(any())
        ).thenReturn(
            cipherSuccess
        )

        subject.authenticate(
            BiometricsType.TYPE_REGISTER, biometricPrompt, promptInfo, callback
        )

        verify(biometricPrompt).authenticate(eq(promptInfo), any())
    }

    @Test
    fun authForRegistration_invalidated() {
        val biometricPrompt = mock<BiometricPrompt>()
        val promptInfo = mock<BiometricPrompt.PromptInfo>()
        val callback = mock<BiometricsCallback<TestBiometricData>>()

        val data = "1234-_-1234" // "MTIzNC1fLTEyMzQ="

        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn(data)

        val cipherInvalidated = CipherState.CipherInvalidatedError(KeyPermanentlyInvalidatedException())
        whenever(
            cryptographyManager.getInitializedCipherForEncryption(any())
        ).thenReturn(
            cipherInvalidated
        )

        subject.authenticate(
            BiometricsType.TYPE_REGISTER, biometricPrompt, promptInfo, callback
        )

        verify(biometricDataRepository).clearBiometricEncryptedData()
        verify(callback).onAuthFailed(any())
    }

    @Test
    fun authForRegistration_other() {
        val biometricPrompt = mock<BiometricPrompt>()
        val promptInfo = mock<BiometricPrompt.PromptInfo>()
        val callback = mock<BiometricsCallback<TestBiometricData>>()

        val data = "1234-_-1234" // "MTIzNC1fLTEyMzQ="

        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn(data)

        val cipherError = CipherState.CipherOtherError(Exception())
        whenever(
            cryptographyManager.getInitializedCipherForEncryption(any())
        ).thenReturn(
            cipherError
        )

        subject.authenticate(
            BiometricsType.TYPE_REGISTER, biometricPrompt, promptInfo, callback
        )

        verify(callback).onAuthFailed(any())
    }

    @Test
    fun authForLogin_success() {
        val biometricPrompt = mock<BiometricPrompt>()
        val promptInfo = mock<BiometricPrompt.PromptInfo>()
        val callback = mock<BiometricsCallback<TestBiometricData>>()

        val data = "1234-_-1234" // "MTIzNC1fLTEyMzQ="

        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn(data)

        val cipherSuccess = CipherState.CipherSuccess(mock())
        whenever(cryptographyManager.getInitializedCipherForDecryption(any(), any())).thenReturn(cipherSuccess)

        subject.authenticate(
            BiometricsType.TYPE_LOGIN, biometricPrompt, promptInfo, callback
        )

        verify(biometricPrompt).authenticate(eq(promptInfo), any())
    }

    @Test
    fun authForLogin_invalidated() {
        val biometricPrompt = mock<BiometricPrompt>()
        val promptInfo = mock<BiometricPrompt.PromptInfo>()
        val callback = mock<BiometricsCallback<TestBiometricData>>()

        val data = "1234-_-1234" // "MTIzNC1fLTEyMzQ="

        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn(data)

        val cipherInvalidation = CipherState.CipherInvalidatedError(KeyPermanentlyInvalidatedException())
        whenever(cryptographyManager.getInitializedCipherForDecryption(any(), any())).thenReturn(cipherInvalidation)

        subject.authenticate(
            BiometricsType.TYPE_LOGIN, biometricPrompt, promptInfo, callback
        )

        verify(biometricDataRepository).clearBiometricEncryptedData()
        verify(callback).onAuthFailed(any())
    }

    @Test
    fun authForLogin_other() {
        val biometricPrompt = mock<BiometricPrompt>()
        val promptInfo = mock<BiometricPrompt.PromptInfo>()
        val callback = mock<BiometricsCallback<TestBiometricData>>()

        val data = "1234-_-1234" // "MTIzNC1fLTEyMzQ="

        whenever(biometricDataRepository.getBiometricEncryptedData()).thenReturn(data)

        val cipherError = CipherState.CipherOtherError(Exception())
        whenever(cryptographyManager.getInitializedCipherForDecryption(any(), any())).thenReturn(cipherError)

        subject.authenticate(
            BiometricsType.TYPE_LOGIN, biometricPrompt, promptInfo, callback
        )

        verify(callback).onAuthFailed(any())
    }

    @Test
    fun cipherState_success() {
        val biometricPrompt = mock<BiometricPrompt>()
        val promptInfo = mock<BiometricPrompt.PromptInfo>()
        val callback = mock<BiometricsCallback<TestBiometricData>>()

        subject.handleCipherStates(CipherState.CipherSuccess(mock()), biometricPrompt, promptInfo, callback)

        verify(biometricPrompt).authenticate(any(), any())
    }

    @Test
    fun cipherState_invalidated() {
        val biometricPrompt = mock<BiometricPrompt>()
        val promptInfo = mock<BiometricPrompt.PromptInfo>()
        val callback = mock<BiometricsCallback<TestBiometricData>>()

        subject.handleCipherStates(
            CipherState.CipherInvalidatedError(KeyPermanentlyInvalidatedException()), biometricPrompt, promptInfo,
            callback
        )

        verify(callback).onAuthFailed(BiometricAuthError.BiometricKeysInvalidated)
    }

    @Test
    fun cipherState_other() {
        val biometricPrompt = mock<BiometricPrompt>()
        val promptInfo = mock<BiometricPrompt.PromptInfo>()
        val callback = mock<BiometricsCallback<TestBiometricData>>()

        subject.handleCipherStates(
            CipherState.CipherInvalidatedError(Exception()), biometricPrompt, promptInfo, callback
        )

        verify(callback).onAuthFailed(any())
    }

    class TestBiometricData(val data: String) : BiometricData {
        override fun asByteArray(): ByteArray =
            data.toByteArray(Charset.forName("UTF-8"))
    }

    class TestBiometricDataFactory : BiometricDataFactory<TestBiometricData> {
        override fun fromByteArray(byteArray: ByteArray): TestBiometricData =
            TestBiometricData(String(byteArray, Charset.forName("UTF-8")))
    }

    companion object {
        private const val DATA_1 = "data_1"
    }
}