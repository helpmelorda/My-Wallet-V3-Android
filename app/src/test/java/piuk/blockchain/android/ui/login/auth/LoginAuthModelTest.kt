package piuk.blockchain.android.ui.login.auth

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.exceptions.DecryptionException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import retrofit2.Response

class LoginAuthModelTest {
    private lateinit var model: LoginAuthModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: LoginAuthInteractor = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = LoginAuthModel(
            initialState = LoginAuthState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `get session ID and payload`() {
        // Arrange
        val sessionId = "SESSION_ID"
        val guid = "GUID"
        val authToken = "TOKEN"
        val responseBody = EMPTY_RESPONSE.toResponseBody(JSON_HEADER.toMediaTypeOrNull())

        whenever(interactor.getSessionId()).thenReturn(sessionId)
        whenever(interactor.authorizeApproval(authToken, sessionId)).thenReturn(
            Single.just(Response.success(responseBody))
        )
        whenever(interactor.getPayload(anyString(), anyString())).thenReturn(
            Single.just(Response.success(responseBody))
        )
        whenever(interactor.getRemaining2FaRetries()).thenReturn(0)

        val testState = model.state.test()
        model.process(LoginAuthIntents.GetSessionId(guid, authToken))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(guid = guid, authToken = authToken, authStatus = AuthStatus.GetSessionId),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.AuthorizeApproval
            ),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.GetPayload
            ),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.GetPayload,
                twoFaState = TwoFaCodeState.TwoFaTimeLock
            ),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.GetPayload,
                payloadJson = EMPTY_RESPONSE,
                twoFaState = TwoFaCodeState.TwoFaTimeLock
            )
        )
    }

    @Test
    fun `auth fail to get payload`() {
        // Arrange
        val sessionId = "SESSION_ID"
        val guid = "GUID"
        val authToken = "TOKEN"
        val responseBody = EMPTY_RESPONSE.toResponseBody(JSON_HEADER.toMediaTypeOrNull())
        whenever(interactor.getSessionId()).thenReturn(sessionId)
        whenever(interactor.authorizeApproval(authToken, sessionId)).thenReturn(
            Single.just(Response.success(responseBody))
        )
        whenever(interactor.getPayload(guid, sessionId)).thenReturn(Single.error(Exception()))

        val testState = model.state.test()
        model.process(LoginAuthIntents.GetSessionId(guid, authToken))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(guid = guid, authToken = authToken, authStatus = AuthStatus.GetSessionId),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.AuthorizeApproval
            ),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.GetPayload
            ),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.AuthFailed
            )
        )
    }

    @Test
    fun `verify password without 2fa`() {
        // Arrange
        val password = "password"
        val isMobileSetup = true
        val deviceType = DEVICE_TYPE_ANDROID
        whenever(interactor.verifyPassword(anyString(), anyString())).thenReturn(
            Completable.complete()
        )
        whenever(interactor.updateMobileSetup(isMobileSetup, deviceType)).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(LoginAuthIntents.VerifyPassword(password))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(
                authStatus = AuthStatus.VerifyPassword,
                password = password
            ),
            LoginAuthState(
                authStatus = AuthStatus.UpdateMobileSetup,
                password = password,
                isMobileSetup = isMobileSetup,
                deviceType = deviceType
            ),
            LoginAuthState(
                authStatus = AuthStatus.Complete,
                password = password,
                isMobileSetup = isMobileSetup,
                deviceType = deviceType
            )
        )
    }

    @Test
    fun `fail to verify password`() {
        // Arrange
        val password = "password"
        whenever(interactor.verifyPassword(anyString(), anyString())).thenReturn(
            Completable.error(
                DecryptionException()
            )
        )

        val testState = model.state.test()
        model.process(LoginAuthIntents.VerifyPassword(password))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(
                authStatus = AuthStatus.VerifyPassword,
                password = password
            ),
            LoginAuthState(
                authStatus = AuthStatus.InvalidPassword,
                password = password
            )
        )
    }

    @Test
    fun `verify password with 2fa`() {
        // Arrange
        val password = "password"
        val twoFACode = "code"
        val isMobileSetup = true
        val deviceType = DEVICE_TYPE_ANDROID
        whenever(interactor.submitCode(anyString(), anyString(), anyString(), anyString())).thenReturn(
            Single.just(TWO_FA_PAYLOAD.toResponseBody((JSON_HEADER).toMediaTypeOrNull()))
        )
        whenever(interactor.verifyPassword(anyString(), anyString())).thenReturn(
            Completable.complete()
        )
        whenever(interactor.updateMobileSetup(isMobileSetup, deviceType)).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(LoginAuthIntents.SubmitTwoFactorCode(password, twoFACode))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(
                authStatus = AuthStatus.Submit2FA,
                password = password,
                code = twoFACode
            ),
            LoginAuthState(
                authStatus = AuthStatus.VerifyPassword,
                password = password,
                code = twoFACode,
                payloadJson = TWO_FA_PAYLOAD
            ),
            LoginAuthState(
                authStatus = AuthStatus.UpdateMobileSetup,
                password = password,
                code = twoFACode,
                payloadJson = TWO_FA_PAYLOAD,
                isMobileSetup = isMobileSetup,
                deviceType = deviceType
            ),
            LoginAuthState(
                authStatus = AuthStatus.Complete,
                password = password,
                code = twoFACode,
                payloadJson = TWO_FA_PAYLOAD,
                isMobileSetup = isMobileSetup,
                deviceType = deviceType
            )
        )
    }

    @Test
    fun `fail to verify 2fa`() {
        val password = "password"
        val twoFACode = "code"
        whenever(interactor.submitCode(anyString(), anyString(), anyString(), anyString())).thenReturn(
            Single.error(Exception())
        )

        val testState = model.state.test()
        model.process(LoginAuthIntents.SubmitTwoFactorCode(password, twoFACode))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(
                authStatus = AuthStatus.Submit2FA,
                password = password,
                code = twoFACode
            ),
            LoginAuthState(
                authStatus = AuthStatus.Invalid2FACode,
                password = password,
                code = twoFACode
            )
        )
    }

    @Test
    fun `request new 2fa code reduces counter`() {
        whenever(interactor.requestNew2FaCode(anyString(), anyString())).thenReturn(
            Completable.complete()
        )

        val retries = 3
        val reducedRetry = 2
        whenever(interactor.getRemaining2FaRetries())
            .thenReturn(retries)
            .thenReturn(reducedRetry)

        val testState = model.state.test()
        model.process(LoginAuthIntents.RequestNew2FaCode)
        model.process(LoginAuthIntents.RequestNew2FaCode)

        testState
            .assertValueAt(0) {
                it == LoginAuthState()
            }
            .assertValueAt(1) {
                it.twoFaState is TwoFaCodeState.TwoFaRemainingTries &&
                    (it.twoFaState as TwoFaCodeState.TwoFaRemainingTries).remainingRetries == retries
            }
            .assertValueAt(2) {
                it.twoFaState is TwoFaCodeState.TwoFaRemainingTries &&
                    (it.twoFaState as TwoFaCodeState.TwoFaRemainingTries).remainingRetries == reducedRetry
            }
    }

    @Test
    fun `request new 2fa retries exhausted`() {
        whenever(interactor.requestNew2FaCode(anyString(), anyString())).thenReturn(
            Completable.complete()
        )

        val retries = 0
        whenever(interactor.getRemaining2FaRetries())
            .thenReturn(retries)

        val testState = model.state.test()
        model.process(LoginAuthIntents.RequestNew2FaCode)

        testState
            .assertValueAt(0) {
                it == LoginAuthState()
            }
            .assertValueAt(1) {
                it.twoFaState is TwoFaCodeState.TwoFaTimeLock
            }
    }

    companion object {

        private const val EMPTY_RESPONSE = "{}"

        private const val JSON_HEADER = "application/json"

        private const val INITIAL_ERROR_MESSAGE = "This is an error"

        private const val INITIAL_ERROR_RESPONSE = "{\"initial_error\":\"$INITIAL_ERROR_MESSAGE\"}"

        private const val TWO_FA_PAYLOAD = "{\"payload\":\"{auth_type: 4}\"}"

        private const val DEVICE_TYPE_ANDROID = 2
    }
}