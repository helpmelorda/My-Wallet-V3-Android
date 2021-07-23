package com.blockchain.nabu.service.wallet

import com.blockchain.nabu.api.wallet.RETAIL_JWT_TOKEN
import com.blockchain.nabu.api.wallet.RetailWallet
import com.blockchain.nabu.models.responses.wallet.RetailJwtResponse
import com.blockchain.nabu.service.RetailWalletTokenService
import com.blockchain.nabu.util.waitForCompletionWithoutErrors
import com.blockchain.testutils.mockWebServerInit
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit

class RetailWalletTokenServiceTest {

    private lateinit var subject: RetailWalletTokenService

    private val retailWallet: RetailWallet = mock()
    private val retrofit: Retrofit = mock()

    private val server: MockWebServer = MockWebServer()
    private val apiKey = "API_KEY"
    private val explorerPath = "explorerPath"

    @get:Rule
    val initMockServer = mockWebServerInit(server)

    @Before
    fun setUp() {
        whenever(retrofit.create(RetailWallet::class.java)).thenReturn(retailWallet)
        subject = RetailWalletTokenService(explorerPath, apiKey, retrofit)
    }

    @Test
    fun `requestJwt`() {
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"

        val expectedResponse = RetailJwtResponse(true, "token", null)

        whenever(
            retailWallet.requestJwt(RETAIL_JWT_TOKEN, guid, sharedKey, apiKey)
        ).thenReturn(
            Single.just(expectedResponse)
        )

        subject.requestJwt(
            path = RETAIL_JWT_TOKEN,
            guid = guid,
            sharedKey = sharedKey
        ).test().waitForCompletionWithoutErrors().assertValue {
            it == expectedResponse
        }
    }
}