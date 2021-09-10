package com.blockchain.nabu.service.nabu

import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.models.responses.nabu.AddAddressRequest
import com.blockchain.nabu.models.responses.nabu.NabuBasicUser
import com.blockchain.nabu.models.responses.nabu.NabuJwt
import com.blockchain.nabu.models.responses.nabu.RecordCountryRequest
import com.blockchain.nabu.models.responses.nabu.RegisterCampaignRequest
import com.blockchain.nabu.models.responses.nabu.Scope
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.nabu.models.responses.nabu.SupportedDocumentsResponse
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenRequest
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.nabu.util.fakefactory.nabu.FakeAddressFactory
import com.blockchain.nabu.util.fakefactory.nabu.FakeNabuCountryFactory
import com.blockchain.nabu.util.fakefactory.nabu.FakeNabuSessionTokenFactory
import com.blockchain.nabu.util.fakefactory.nabu.FakeNabuUserFactory
import com.blockchain.testutils.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Test

class NabuServiceTest {

    private val nabu: Nabu = mock()
    private val subject: NabuService = NabuService(nabu)

    private val jwt = "JWT"

    @Test
    fun getAuthToken() {
        val expectedTokenResponse = NabuOfflineTokenResponse(
            "d753109e-34c2-42bd-82f1-cc90470234kf",
            "d753109e-23jd-42bd-82f1-cc904702asdfkjf"
        )

        whenever(
            nabu.getAuthToken(NabuOfflineTokenRequest(jwt))
        ).thenReturn(
            Single.just(
                expectedTokenResponse
            )
        )

        subject.getAuthToken(jwt).test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.userId == expectedTokenResponse.userId
                it.token == expectedTokenResponse.token
            }
    }

    @Test
    fun getSessionToken() {
        val guid = "GUID"
        val email = "EMAIL"
        val userId = "USER_ID"
        val offlineToken = "OFFLINE_TOKEN"
        val appVersion = "6.14.0"
        val deviceId = "DEVICE_ID"

        val expectedSessionTokenResponse = FakeNabuSessionTokenFactory.any

        whenever(
            nabu.getSessionToken(userId, offlineToken, guid, email, appVersion, "APP", deviceId)
        ).thenReturn(
            Single.just(expectedSessionTokenResponse)
        )

        subject.getSessionToken(
            userId,
            offlineToken,
            guid,
            email,
            appVersion,
            deviceId
        ).test().waitForCompletionWithoutErrors().assertValue {
            it == expectedSessionTokenResponse
        }
    }

    @Test
    fun createBasicUser() {
        val firstName = "FIRST_NAME"
        val lastName = "LAST_NAME"
        val dateOfBirth = "12-12-1234"
        val sessionToken = FakeNabuSessionTokenFactory.any

        whenever(
            nabu.createBasicUser(NabuBasicUser(firstName, lastName, dateOfBirth), sessionToken.authHeader)
        ).thenReturn(
            Completable.complete()
        )

        subject.createBasicUser(
            firstName,
            lastName,
            dateOfBirth,
            sessionToken
        ).test().waitForCompletionWithoutErrors()
    }

    @Test
    fun getUser() {
        val sessionToken = FakeNabuSessionTokenFactory.any
        val expectedUser = FakeNabuUserFactory.satoshi

        whenever(
            nabu.getUser(sessionToken.authHeader)
        ).thenReturn(
            Single.just(expectedUser)
        )

        subject.getUser(sessionToken).test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it == expectedUser
            }
    }

    @Test
    fun updateWalletInformation() {
        val sessionToken = FakeNabuSessionTokenFactory.any
        val expectedUser = FakeNabuUserFactory.satoshi

        whenever(
            nabu.updateWalletInformation(NabuJwt(jwt), sessionToken.authHeader)
        ).thenReturn(
            Single.just(expectedUser)
        )
        subject.updateWalletInformation(sessionToken, jwt).test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it == expectedUser
            }
    }

    @Test
    fun addAddress() {
        val addressToAdd = FakeAddressFactory.any
        val sessionToken = FakeNabuSessionTokenFactory.any

        whenever(
            nabu.addAddress(AddAddressRequest(addressToAdd), sessionToken.authHeader)
        ).thenReturn(
            Completable.complete()
        )

        subject.addAddress(
            sessionToken,
            addressToAdd.line1!!,
            addressToAdd.line2,
            addressToAdd.city!!,
            addressToAdd.state,
            addressToAdd.postCode,
            addressToAdd.countryCode!!
        ).test().waitForCompletionWithoutErrors()
    }

    @Test
    fun recordCountrySelection() {
        val countryCode = "US"
        val state = "US-AL"
        val notifyWhenAvailable = true

        val sessionToken = FakeNabuSessionTokenFactory.any

        whenever(
            nabu.recordSelectedCountry(
                RecordCountryRequest(jwt, countryCode, notifyWhenAvailable, state),
                sessionToken.authHeader
            )
        ).thenReturn(
            Completable.complete()
        )

        subject.recordCountrySelection(
            sessionToken,
            jwt,
            countryCode,
            state,
            notifyWhenAvailable
        ).test().waitForCompletionWithoutErrors()
    }

    @Test
    fun `get kyc countries`() {
        val scope = Scope.Kyc
        val expectedCountryList = FakeNabuCountryFactory.list

        whenever(
            nabu.getCountriesList(scope.value)
        ).thenReturn(
            Single.just(expectedCountryList)
        )
        subject.getCountriesList(scope).test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it == expectedCountryList
            }
    }

    @Test
    fun `get all countries with no scope`() {
        val scope = Scope.None
        val expectedCountryList = FakeNabuCountryFactory.list

        whenever(
            nabu.getCountriesList(scope.value)
        ).thenReturn(
            Single.just(expectedCountryList)
        )

        subject.getCountriesList(scope).test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it == expectedCountryList
            }
    }

    @Test
    fun getSupportedDocuments() {
        val countryCode = "US"
        val sessionToken = FakeNabuSessionTokenFactory.any
        val expectedDocuments = arrayListOf(SupportedDocuments.DRIVING_LICENCE, SupportedDocuments.PASSPORT)

        whenever(
            nabu.getSupportedDocuments(countryCode, sessionToken.authHeader)
        ).thenReturn(
            Single.just(SupportedDocumentsResponse(countryCode, expectedDocuments))
        )

        subject.getSupportedDocuments(
            sessionToken,
            countryCode
        ).test().waitForCompletionWithoutErrors().assertValue {
            it == expectedDocuments
        }
    }

    @Test
    fun `recover user`() {
        val userId = "userID"
        val offlineToken = NabuOfflineTokenResponse(userId, "token")

        whenever(
            nabu.recoverUser(userId, NabuJwt(jwt), "Bearer ${offlineToken.token}")
        ).thenReturn(
            Completable.complete()
        )

        subject.recoverUser(offlineToken, jwt).test().waitForCompletionWithoutErrors()
    }

    @Test
    fun `register for campaign`() {
        val sessionToken = FakeNabuSessionTokenFactory.any
        val campaignName = "name"
        val campaignRequest = RegisterCampaignRequest(
            mapOf("key" to "value"),
            true
        )

        whenever(
            nabu.registerCampaign(campaignRequest, campaignName, sessionToken.authHeader)
        ).thenReturn(
            Completable.complete()
        )

        subject.registerCampaign(
            sessionToken,
            campaignRequest,
            campaignName
        ).test().waitForCompletionWithoutErrors()
    }
}