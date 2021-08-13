package info.blockchain.wallet.api

import com.blockchain.nabu.util.waitForCompletionWithoutErrors
import com.blockchain.testutils.FakeHttpExceptionFactory
import com.blockchain.testutils.getStringFromResource
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.api.data.SignedToken
import info.blockchain.wallet.api.data.Status
import info.blockchain.wallet.api.data.WalletOptions
import info.blockchain.wallet.payload.data.WalletBase
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.amshove.kluent.`should be equal to`
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import retrofit2.Call
import retrofit2.Response
import java.security.SecureRandom

class WalletApiTest {
    private val walletExplorerEndpoints: WalletExplorerEndpoints = mock()
    private val apiCode: ApiCode = mock()
    private val captchaSiteKey: String = "captchaSiteKey"
    private val subject: WalletApi = WalletApi(walletExplorerEndpoints, apiCode, captchaSiteKey)

    @Test
    fun `get encrypted payload`() {
        val guid = "a09910d9-1906-4ea1-a956-2508c3fe0661"
        val sessionId = ""
        val encryptedPayload = getStringFromResource("encrypted-payload.txt")
        val response = Response.success(encryptedPayload.toResponseBody("plain/text".toMediaTypeOrNull()))

        whenever(
            walletExplorerEndpoints.fetchEncryptedPayload(
                guid,
                "SID=$sessionId",
                "json",
                true,
                apiCode.apiCode
            )
        ).thenReturn(
            Observable.just(response)
        )

        subject.fetchEncryptedPayload(guid, sessionId).test()
            .waitForCompletionWithoutErrors().assertValue {
                WalletBase.fromJson(it.body()!!.string()).guid == "a09910d9-1906-4ea1-a956-2508c3fe0661"
            }
    }

    @Test
    fun `get encrypted payload invalid guid`() {
        val guid = "a09910d9-1906-4ea1-a956-2508c3fe0661"
        val sessionId = ""

        val expectedResponse: Response<ResponseBody> = FakeHttpExceptionFactory.mockResponse(
            "".toResponseBody("plain/text".toMediaTypeOrNull())
        )

        whenever(
            walletExplorerEndpoints.fetchEncryptedPayload(
                guid,
                "SID=$sessionId",
                "json",
                true,
                apiCode.apiCode
            )
        ).thenReturn(
            Observable.just(expectedResponse)
        )

        subject.fetchEncryptedPayload(guid, "").test()
            .waitForCompletionWithoutErrors().assertValue {
                it == expectedResponse
            }
    }

    @Test
    fun `get pairing encryption password`() {
        val guid = "a09910d9-1906-4ea1-a956-2508c3fe0661"
        val expectedBodyString = "5001071ac0ea0b6993444716729429c1d7637def2bcc73a6ad6360c9cec06d47"

        val callResponse: Call<ResponseBody> = FakeHttpExceptionFactory.mockApiCall(
            expectedBodyString.toResponseBody("plain/text".toMediaTypeOrNull())
        )
        whenever(
            walletExplorerEndpoints.fetchPairingEncryptionPasswordCall(
                "pairing-encryption-password",
                guid,
                apiCode.apiCode
            )
        ).thenReturn(
            callResponse
        )
        val bodyString = subject.fetchPairingEncryptionPasswordCall(guid).execute()
            .body()!!.string()

        bodyString `should be equal to` expectedBodyString
    }

    @Test
    fun `get Ethereum options`() {
        val expectedWalletOptions = WalletOptions()

        whenever(
            walletExplorerEndpoints.getWalletOptions(apiCode.apiCode)
        ).thenReturn(
            Observable.just(expectedWalletOptions)
        )
        subject.walletOptions.test()
            .waitForCompletionWithoutErrors().assertValue {
                it == expectedWalletOptions
            }
    }

    @Test
    fun `set access`() {
        val pin = "1234"
        val bytes = ByteArray(16)
        val random = SecureRandom()
        random.nextBytes(bytes)
        val key = bytes.toString(Charsets.UTF_8)
        random.nextBytes(bytes)
        val value = bytes.toString(Charsets.UTF_8)
        val expectedResponse: Response<Status> = mock()
        whenever(
            walletExplorerEndpoints.pinStore(
                key,
                pin,
                Hex.toHexString(value.toByteArray()),
                "put",
                apiCode.apiCode
            )
        ).thenReturn(
            Observable.just(expectedResponse)
        )

        subject.setAccess(key, value, pin).test()
            .waitForCompletionWithoutErrors().assertValue {
                it == expectedResponse
            }
    }

    @Test
    fun `validate access`() {
        val key = "db2f4184429bf05c1a962384befb8873"
        val keyReturned = "3236346436663830663565363434383130393262343739613437333763333739"
        val pin = "1234"

        whenever(
            walletExplorerEndpoints.pinStore(
                key,
                pin,
                null,
                "get",
                apiCode.apiCode
            )
        ).thenReturn(
            Observable.just(Response.success(200, Status.fromJson("{ \"success\": \"$keyReturned\"}")))
        )

        subject.validateAccess(key, pin).test()
            .waitForCompletionWithoutErrors().assertValue {
                it.body()!!.success == keyReturned
            }
    }

    @Test
    fun `get signed Json token should be successful`() {
        val verifiedGuid = "cc3b7469-2b45-4af6-ac49-480d75a70d0f"
        val verifiedSharedKey = "7dc0efed-a548-4732-8488-7bbb3f345f9b"
        val partner = "coinify"

        val expectedToken = SignedToken(true, "token", null)
        whenever(
            walletExplorerEndpoints.getSignedJsonToken(
                verifiedGuid,
                verifiedSharedKey,
                "email%7Cwallet_age",
                partner, apiCode.apiCode
            )
        ).thenReturn(
            Single.just(expectedToken)
        )

        subject.getSignedJsonToken(verifiedGuid, verifiedSharedKey, partner).test()
            .waitForCompletionWithoutErrors().assertValue {
                it == expectedToken.token
            }
    }
}