package piuk.blockchain.androidcore.data.auth

import androidx.annotation.VisibleForTesting
import com.blockchain.api.services.AuthApiService
import com.blockchain.logging.CrashLogger
import info.blockchain.wallet.api.data.WalletOptions
import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.ServerConnectionException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.exceptions.Exceptions
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.utils.AESUtilWrapper
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.handleResponse
import piuk.blockchain.androidcore.utils.extensions.isValidPin
import retrofit2.Response
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class AuthDataManager(
    private val prefs: PersistentPrefs,
    private val authApiService: AuthApiService,
    private val walletAuthService: WalletAuthService,
    private val accessState: AccessState,
    private val aesUtilWrapper: AESUtilWrapper,
    private val prngHelper: PrngFixer,
    private val crashLogger: CrashLogger
) {

    @VisibleForTesting
    internal var timer: Int = 0

    private var shouldVerifyCloudBackup = false

    /**
     * Returns a [WalletOptions] object from the website. This object is used to get the
     * current buy/sell partner info as well as a list of countries where buy/sell is rolled out.
     *
     * @return An [Observable] wrapping a [WalletOptions] object
     */
    fun getWalletOptions(): Observable<WalletOptions> =
        walletAuthService.getWalletOptions()
            .applySchedulers()

    /**
     * Attempts to retrieve an encrypted Payload from the server, but may also return just part of a
     * Payload or an error response.
     *
     * @param guid The user's unique GUID
     * @param sessionId The current session ID
     * @return An [Observable] wrapping a [<] which could notify
     * the user that authentication (ie checking your email, 2FA etc) is required
     * @see .getSessionId
     */
    fun getEncryptedPayload(guid: String, sessionId: String): Observable<Response<ResponseBody>> =
        walletAuthService.getEncryptedPayload(guid, sessionId)
            .applySchedulers()

    fun getEncryptedPayloadObject(guid: String, sessionId: String): Single<JsonObject> =
        walletAuthService.getEncryptedPayload(guid, sessionId)
            .applySchedulers()
            .firstOrError()
            .flatMap {
                it.handleResponse()
            }

    /**
     * Gets an ephemeral session ID from the server.
     *
     * @param guid The user's unique GUID
     * @return An [Observable] wrapping a session ID as a String
     */
    fun getSessionId(guid: String): Observable<String> =
        walletAuthService.getSessionId(guid)
            .applySchedulers()

    /**
     * Create a session ID for the given email for authorization
     *
     * @param email The user's email
     * @return A [Single] wrapping the result
     */
    fun createSessionId(email: String): Single<ResponseBody> =
        walletAuthService.createSessionId(email)

    /**
     * Requests authorization for the session specified by the ID
     *
     * @param authToken The token required for auth from the email
     * @param sessionId The current session ID
     * @return A [Single] wrapping
     */
    fun authorizeSessionObject(authToken: String, sessionId: String): Single<JsonObject> =
        walletAuthService.authorizeSession(authToken, sessionId).flatMap { it.handleResponse() }

    /**
     * Submits a user's 2FA code to the server and returns a response. This response will contain
     * the user's encrypted Payload if successful, if not it will contain an error.
     *
     * @param sessionId The current session ID
     * @param guid The user's unique GUID
     * @param twoFactorCode A valid 2FA code generated from Google Authenticator or similar
     * @see .getSessionId
     */
    fun submitTwoFactorCode(
        sessionId: String,
        guid: String,
        twoFactorCode: String
    ): Observable<ResponseBody> = walletAuthService.submitTwoFactorCode(sessionId, guid, twoFactorCode)
        .applySchedulers()

    /**
     * Polls for the auth status of a user's account every 2 seconds until either the user checks
     * their email and a valid Payload is returned, or the call fails.
     *
     * @param guid The user's unique GUID
     * @param sessionId The current session ID
     * @return An [Observable] wrapping a String which represents the user's Payload OR an
     * auth required response from the API
     */
    fun startPollingAuthStatus(guid: String, sessionId: String): Observable<String> {
        // Emit tick every 2 seconds
        return Observable.interval(2, TimeUnit.SECONDS)
            // For each emission from the timer, try to get the payload
            .map { getEncryptedPayload(guid, sessionId).blockingFirst() }
            // If auth not required, emit payload
            .filter { s ->
                s.errorBody() == null ||
                        !s.errorBody()!!.string().contains(AUTHORIZATION_REQUIRED)
            }
            // Return message in response
            .map { responseBodyResponse -> responseBodyResponse.body()!!.string() }
            // If error called, emit Auth Required
            .onErrorReturn { AUTHORIZATION_REQUIRED }
            // Only emit the first object
            .firstElement()
            // As Observable rather than Maybe
            .toObservable()
            // Apply correct threading
            .applySchedulers()
    }

    /**
     * Creates a timer which counts down for two minutes and emits the remaining time on each count.
     * This is used to show the user how long they have to check their email before the login
     * request expires.
     *
     * @return An [Observable] where the emitted int is the number of seconds left
     */
    fun createCheckEmailTimer(): Observable<Int> {
        timer = 2 * 60

        return Observable.interval(0, 1, TimeUnit.SECONDS)
            .map { timer-- }
            .takeUntil { timer < 0 }
    }

    /**
     * Validates the passed PIN for the user's GUID and shared key and returns a decrypted password.
     *
     * @param passedPin The PIN to be used
     * @return An [Observable] where the wrapped String is the user's decrypted password
     */
    fun validatePin(passedPin: String): Observable<String> {
        return getValidatePinObservable(passedPin)
            .applySchedulers()
    }

    /**
     * Creates a new PIN for a user
     *
     * @param password The user's password
     * @param passedPin The new chosen PIN
     * @return A [Completable] object
     */
    fun createPin(password: String, passedPin: String): Completable {
        return getCreatePinObservable(password, passedPin)
            .applySchedulers()
    }

    private fun getValidatePinObservable(passedPin: String): Observable<String> {
        val key = prefs.pinId

        if (!passedPin.isValidPin()) {
            return Observable.error(IllegalArgumentException("Invalid PIN"))
        } else {
            accessState.setPin(passedPin)
            crashLogger.logEvent("validatePin. pin set. validity: ${passedPin.isValidPin()}")
        }

        return walletAuthService.validateAccess(key, passedPin)
            .map { response ->
                /*
                Note: Server side issue - If the incorrect PIN is supplied the server will respond
                with a 403 { code: 1, error: "Incorrect PIN you have x attempts left" }
                 */
                if (response.isSuccessful) {
                    accessState.isNewlyCreated = false
                    accessState.isRestored = false
                    val decryptionKey = response.body()!!.success

                    handleBackup(decryptionKey)

                    return@map aesUtilWrapper.decrypt(
                        prefs.encryptedPassword,
                        decryptionKey,
                        AESUtil.PIN_PBKDF2_ITERATIONS
                    )
                } else {
                    if (response.code() == 403) {
                        // Invalid PIN
                        throw InvalidCredentialsException("Validate access failed")
                    } else {
                        throw ServerConnectionException("""${response.code()} ${response.message()}""")
                    }
                }
            }
    }

    /*
    This function takes care of saving the encrypted values into the special storage for
    the automatic backup, and also decrypts the values when necessary into local storage.
     */
    private fun handleBackup(decryptionKey: String) {
        shouldVerifyCloudBackup = when {
            // Just to make sure, if the user specifically opted out out of cloud backups,
            // always clear the backup over here.
            !prefs.backupEnabled -> {
                prefs.clearBackup()
                false
            }
            prefs.hasBackup() && prefs.walletGuid.isEmpty() -> {
                prefs.restoreFromBackup(decryptionKey, aesUtilWrapper)
                false
            }
            else -> {
                prefs.backupCurrentPrefs(decryptionKey, aesUtilWrapper)
                true
            }
        }
    }

    private fun getCreatePinObservable(password: String, passedPin: String): Completable {

        if (!passedPin.isValidPin()) {
            return Completable.error(IllegalArgumentException("Invalid PIN"))
        } else {
            accessState.setPin(passedPin)
            crashLogger.logEvent("createPin. pin set. validity: ${passedPin.isValidPin()}")
        }

        prngHelper.applyPRNGFixes()

        return Completable.create { subscriber ->
            val bytes = ByteArray(16)
            val random = SecureRandom()
            random.nextBytes(bytes)
            val key = String(Hex.encode(bytes), Charsets.UTF_8)
            random.nextBytes(bytes)
            val value = String(Hex.encode(bytes), Charsets.UTF_8)

            walletAuthService.setAccessKey(key, value, passedPin)
                .subscribe({ response ->
                    if (response.isSuccessful) {
                        val encryptionKey = Hex.toHexString(value.toByteArray(Charsets.UTF_8))

                        val encryptedPassword = aesUtilWrapper.encrypt(
                            password,
                            encryptionKey,
                            AESUtil.PIN_PBKDF2_ITERATIONS
                        )

                        prefs.encryptedPassword = encryptedPassword
                        prefs.pinId = key

                        handleBackup(encryptionKey)

                        if (!subscriber.isDisposed) {
                            subscriber.onComplete()
                        }
                    } else {
                        throw Exceptions.propagate(
                            Throwable("Validate access failed: ${response.errorBody()?.string()}")
                        )
                    }
                }) { throwable ->
                    if (!subscriber.isDisposed) {
                        subscriber.onError(throwable)
                        subscriber.onComplete()
                    }
                }
        }
    }

    /**
     * Get the encryption password for pairing
     *
     * @param guid A user's GUID
     * @return [<] wrapping the pairing encryption password
     */
    fun getPairingEncryptionPassword(guid: String): Observable<ResponseBody> =
        walletAuthService.getPairingEncryptionPassword(guid)
            .applySchedulers()

    /**
    * Send email to verify device
    *
    * @param sessionId The token for the current session
    * @param email The user's email
    * @param captcha Captcha token
    * @return A [Single] wrapping the result
    */
    fun sendEmailForDeviceVerification(sessionId: String, email: String, captcha: String): Single<ResponseBody> =
        walletAuthService.sendEmailForDeviceVerification(sessionId, email, captcha)

    /**
     * Update the account model fields for mobile setup
     *
     * @param guid The user's GUID
     * @param sharedKey The shared key of the specified GUID
     * @param isMobileSetup has mobile device linked
     * @param deviceType the type of the linked device: 1 - iOS, 2 - Android
     * @return A [Single] wrapping the result
     */
    fun updateMobileSetup(
        guid: String,
        sharedKey: String,
        isMobileSetup: Boolean,
        deviceType: Int
    ): Single<ResponseBody> = walletAuthService.updateMobileSetup(guid, sharedKey, isMobileSetup, deviceType)

    /**
     * Update the mnemonic backup date (calculated on the backend)
     *
     * @return A [Completable] wrapping the result
     */
    fun updateMnemonicBackup(): Completable =
        walletAuthService.updateMnemonicBackup(prefs.walletGuid, prefs.sharedKey)
            .applySchedulers()

    /**
     * Verify that the cloud backup has been completed
     *
     * @return A [Completable] wrapping the result
     */
    fun verifyCloudBackup(): Completable = if (shouldVerifyCloudBackup) {
        Completable.fromSingle(
            walletAuthService.verifyCloudBackup(
                guid = prefs.walletGuid,
                sharedKey = prefs.sharedKey,
                hasCloudBackup = true,
                deviceType = DEVICE_TYPE_ANDROID
            )
        ).applySchedulers()
    } else {
        Completable.complete()
    }

    /**
     * Send email to verify device via the extended magic link (including the recovery token)
     *
     * @param sessionId The token for the current session
     * @param email The user's email
     * @param captcha Captcha token
     * @return A [Completable] wrapping the result
     */
    fun sendEmailForAuthentication(sessionId: String, email: String, captcha: String) =
        authApiService.sendEmailForAuthentication(
            sessionId = sessionId,
            email = email,
            captcha = captcha
        )

    companion object {
        @VisibleForTesting
        internal const val AUTHORIZATION_REQUIRED = "authorization_required"
        private const val DEVICE_TYPE_ANDROID = 2
    }
}
