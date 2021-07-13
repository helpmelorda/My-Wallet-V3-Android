package com.blockchain.nabu.service.wallet

import com.blockchain.nabu.api.wallet.RETAIL_JWT_TOKEN
import com.blockchain.nabu.models.responses.nabu.KycStateAdapter
import com.blockchain.nabu.models.responses.nabu.UserStateAdapter
import com.blockchain.nabu.service.RetailWalletTokenService
import com.blockchain.testutils.MockedRetrofitTest
import com.blockchain.testutils.getStringFromResource
import com.blockchain.testutils.mockWebServerInit
import com.squareup.moshi.Moshi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be equal to`
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RetailWalletTokenServiceTest {

    private lateinit var subject: RetailWalletTokenService
    private val moshi: Moshi = Moshi.Builder()
        .add(UserStateAdapter())
        .add(KycStateAdapter())
        .build()
    private val server: MockWebServer = MockWebServer()
    private val apiKey = "API_KEY"

    @get:Rule
    val initMockServer = mockWebServerInit(server)

    @Before
    fun setUp() {
        subject = RetailWalletTokenService(
            "explorer_api",
            apiKey,
            MockedRetrofitTest(moshi, server).retrofit
        )
    }

    @Test
    fun `requestJwt success`() {
        // Arrange
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/wallet/GetSignedTokenSuccess.json"))
        )
        // Act
        val testObserver = subject.requestJwt(
            path = RETAIL_JWT_TOKEN,
            guid = guid,
            sharedKey = sharedKey
        ).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val jwtResponse = testObserver.values().first()
        jwtResponse.isSuccessful `should be equal to` true
        jwtResponse.token `should be equal to` "TOKEN"
        jwtResponse.error `should be equal to` null
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$RETAIL_JWT_TOKEN?guid=$guid&sharedKey=$sharedKey&api_code=$apiKey"
    }

    @Test
    fun `requestJwt failure`() {
        // Arrange
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/wallet/GetSignedTokenFailure.json"))
        )
        // Act
        val testObserver = subject.requestJwt(
            path = RETAIL_JWT_TOKEN,
            guid = guid,
            sharedKey = sharedKey
        ).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val jwtResponse = testObserver.values().first()
        jwtResponse.isSuccessful `should be equal to` false
        jwtResponse.error `should be equal to` "ERROR"
        jwtResponse.token `should be equal to` null
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$RETAIL_JWT_TOKEN?guid=$guid&sharedKey=$sharedKey&api_code=$apiKey"
    }
}