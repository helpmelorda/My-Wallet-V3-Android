package com.blockchain.biometrics

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

interface CryptographyManager {

    /**
     * This method first gets or generates an instance of SecretKey and then initializes the Cipher
     * with the key. The secret key uses [ENCRYPT_MODE][Cipher.ENCRYPT_MODE] is used.
     *
     * @param requireUserAuthentication specifies whether or not this cipher requires biometric confirmation from user
     */
    fun getInitializedCipherForEncryption(keyName: String, requireUserAuthentication: Boolean): CipherState

    /**
     * This method returns the cipher used to encrypt data based on the keyName and separator originally used.
     *
     * @param requireUserAuthentication specifies whether or not this cipher requires biometric confirmation from user
     */
    fun getInitializedCipherForDecryption(
        keyName: String,
        separator: String,
        encryptedData: String,
        requireUserAuthentication: Boolean
    ): CipherState

    /**
     * The Cipher created with [getInitializedCipherForEncryption] is used here
     */
    fun encryptData(byteArray: ByteArray, cipher: Cipher): EncryptedData

    /**
     * The Cipher created with [getInitializedCipherForDecryption] is used here
     */
    fun decryptData(ciphertext: ByteArray, cipher: Cipher): ByteArray

    fun clearData(keyName: String)

    /**
     * Encrypt and encode data along with the initialization vector.  Generates a cipher based on the keyName provided.
     * NOTE: This encrypts data without user confirmation.  If user confirmation is required, use
     * [getInitializedCipherForEncryption] to obtain a cipher instead
     * @throws CipherStateException if the cipher could not be created
     */
    fun encryptAndEncodeData(
        keyName: String,
        separator: String,
        unencryptedData: ByteArray
    ): String

    /**
     * Encrypt and encode data along with the initialization vector.
     *
     * @param separator The separator used to join the encrypted data and initialization vector
     */
    fun encryptAndEncodeData(
        cipher: Cipher,
        separator: String,
        unencryptedData: ByteArray
    ): String

    /**
     * Decode and decrypt data that was initially encrypted/encoded.  Generates a cipher based on the keyName provided.
     * NOTE: This encrypts data without user confirmation.  If user confirmation is required, use
     * [getInitializedCipherForDecryption] to obtain a cipher instead
     * @throws CipherStateException if the cipher could not be created
     */
    fun decodeAndDecryptData(
        keyName: String,
        separator: String,
        encryptedData: String
    ): ByteArray

    /**
     * Decode and decrypt data that was initially encrypted/encoded.  See [encryptAndEncodeData]
     *
     * @param separator The separator used to join the encrypted data and initialization vector
     */
    fun decodeAndDecryptData(
        cipher: Cipher,
        separator: String,
        encryptedData: String
    ): ByteArray
}

data class EncryptedData(val ciphertext: ByteArray, val initializationVector: ByteArray)

sealed class CipherState {
    data class CipherSuccess(val cipher: Cipher) : CipherState()
    data class CipherInvalidatedError(val e: Throwable) : CipherState()
    data class CipherNoSuitableBiometrics(val e: Throwable) : CipherState()
    data class CipherOtherError(val e: Throwable) : CipherState()
}

class CipherStateException(val cipherState: CipherState) : Exception()

class CryptographyManagerImpl() : CryptographyManager {
    private val KEY_SIZE: Int = 256
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES

    override fun getInitializedCipherForEncryption(keyName: String, requireUserAuthentication: Boolean): CipherState =
        initialiseCipher(Cipher.ENCRYPT_MODE, keyName, requireUserAuthentication)

    override fun getInitializedCipherForDecryption(
        keyName: String,
        separator: String,
        encryptedData: String,
        requireUserAuthentication: Boolean
    ): CipherState {
        val ivSpec = decodeFromBase64ToArray(getDataAndIV(encryptedData, separator).second)
        return initialiseCipher(Cipher.DECRYPT_MODE, keyName, requireUserAuthentication, ivSpec)
    }

    private fun initialiseCipher(
        mode: Int,
        keyName: String,
        requireUserAuthentication: Boolean,
        initializationVector: ByteArray? = null
    ): CipherState {
        val cipher = getCipher()
        return try {
            val secretKey = getOrCreateSecretKey(keyName, requireUserAuthentication)
            if (mode == Cipher.ENCRYPT_MODE) {
                cipher.init(mode, secretKey)
            } else if (mode == Cipher.DECRYPT_MODE) {
                require(initializationVector != null)
                cipher.init(mode, secretKey, IvParameterSpec(initializationVector))
            }
            CipherState.CipherSuccess(cipher)
        } catch (e: KeyPermanentlyInvalidatedException) {
            removeKey(keyName)
            CipherState.CipherInvalidatedError(e)
        } catch (e: InvalidAlgorithmParameterException) {
            removeKey(keyName)
            CipherState.CipherNoSuitableBiometrics(e)
        } catch (e: Exception) {
            CipherState.CipherOtherError(e)
        }
    }

    private fun removeKey(keyName: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        if (keyStore.containsAlias(keyName)) {
            keyStore.deleteEntry(keyName)
        }
    }

    override fun clearData(keyName: String) {
        removeKey(keyName)
    }

    override fun encryptData(byteArray: ByteArray, cipher: Cipher): EncryptedData {
        val ciphertext = cipher.doFinal(byteArray)
        return EncryptedData(ciphertext, cipher.iv)
    }

    override fun decryptData(ciphertext: ByteArray, cipher: Cipher): ByteArray =
        cipher.doFinal(ciphertext)

    private fun getCipher(): Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }

    private fun getOrCreateSecretKey(keyName: String, requireUserAuthentication: Boolean): SecretKey {
        // If Secretkey was previously created for that keyName, then grab and return it.
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        if (keyStore.containsAlias(keyName)) {
            return keyStore.getKey(keyName, null) as SecretKey
        }

        // if you reach here, then a new SecretKey must be generated for that keyName
        val paramsBuilder = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        paramsBuilder.apply {
            setBlockModes(ENCRYPTION_BLOCK_MODE)
            setEncryptionPaddings(ENCRYPTION_PADDING)
            setKeySize(KEY_SIZE)
            setUserAuthenticationRequired(requireUserAuthentication)
            if (requireUserAuthentication && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                paramsBuilder.setInvalidatedByBiometricEnrollment(true)
            }
        }

        val keyGenParams = paramsBuilder.build()
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(keyGenParams)
        return keyGenerator.generateKey()
    }

    private fun getEncryptionCipher(keyName: String) =
        when (val cipherState = getInitializedCipherForEncryption(keyName, false)) {
            is CipherState.CipherSuccess -> {
                cipherState.cipher
            }
            is CipherState.CipherOtherError,
            is CipherState.CipherNoSuitableBiometrics,
            is CipherState.CipherInvalidatedError -> {
                throw CipherStateException(cipherState)
            }
        }

    override fun encryptAndEncodeData(
        keyName: String,
        separator: String,
        unencryptedData: ByteArray
    ): String =
        encryptAndEncodeData(getEncryptionCipher(keyName), separator, unencryptedData)

    override fun encryptAndEncodeData(
        cipher: Cipher,
        separator: String,
        unencryptedData: ByteArray
    ): String =
        with(encryptData(unencryptedData, cipher)) {
            generateCompositeKey(ciphertext, separator, initializationVector)
        }

    private fun getDecryptionCipher(keyName: String, separator: String, encryptedData: String) =
        when (val cipherState = getInitializedCipherForDecryption(keyName, separator, encryptedData, false)) {
            is CipherState.CipherSuccess -> {
                cipherState.cipher
            }
            is CipherState.CipherOtherError,
            is CipherState.CipherNoSuitableBiometrics,
            is CipherState.CipherInvalidatedError -> {
                throw CipherStateException(cipherState)
            }
        }

    override fun decodeAndDecryptData(
        keyName: String,
        separator: String,
        encryptedData: String
    ): ByteArray =
        decodeAndDecryptData(getDecryptionCipher(keyName, separator, encryptedData), separator, encryptedData)

    override fun decodeAndDecryptData(
        cipher: Cipher,
        separator: String,
        encryptedData: String
    ): ByteArray =
        with(getDataAndIV(encryptedData, separator)) {
            decryptData(decodeFromBase64ToArray(this.first), cipher)
        }

    internal fun generateCompositeKey(encryptedText: ByteArray, separator: String, initializationVector: ByteArray) =
        encodeToBase64(encryptedText) + separator + encodeToBase64(initializationVector)

    private fun encodeToBase64(data: ByteArray) =
        Base64.encodeToString(data, Base64.DEFAULT)

    internal fun decodeFromBase64ToArray(data: String): ByteArray =
        Base64.decode(data, Base64.DEFAULT)

    internal fun getDataAndIV(data: String, separator: String): Pair<String, String> {
        if (!data.contains(separator)) {
            throw IllegalStateException("Passed data does not contain expected separator")
        }

        val split = data.split(separator.toRegex())
        if (split.size != 2 || (split.size == 2 && split[1].isEmpty())) {
            throw IllegalArgumentException("Passed data is incorrect. There was no IV specified with it.")
        }
        return Pair(split[0], split[1])
    }
}