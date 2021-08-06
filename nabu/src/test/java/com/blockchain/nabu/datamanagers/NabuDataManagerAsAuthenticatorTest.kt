package com.blockchain.nabu.datamanagers

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.reactivex.rxjava3.core.Single

import org.amshove.kluent.`should be equal to`
import org.junit.Rule
import org.junit.Test

class NabuDataManagerAsAuthenticatorTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
    }

    @Test
    fun `the token is fetched and passed to the manager`() {

        val token = givenToken("User", "ABC")

        val nabuDataManager = mock<NabuDataManager>()
        val sut = NabuAuthenticator(token, nabuDataManager, mock()) as Authenticator

        val theFunction = mock<(NabuSessionTokenResponse) -> Single<Int>>()
        sut.authenticate(theFunction)
            .test()

        verify(nabuDataManager).authenticate(
            eq(NabuOfflineTokenResponse("User", "ABC")),
            any<(NabuSessionTokenResponse) -> Single<Int>>()
        )
        verifyNoMoreInteractions(nabuDataManager)
    }

    @Test
    fun `the token is fetched and passed to the manager during the authenticate Single Token overload`() {

        val token = givenToken("User", "ABC")

        val nabuDataManager = mock<NabuDataManager> {
            on { currentToken(NabuOfflineTokenResponse("User", "ABC")) }.thenReturn(
                Single.just(
                    nabuSessionTokenResponse("User", "ABC")
                )
            )
        }
        val sut = NabuAuthenticator(token, nabuDataManager, mock()) as Authenticator

        sut.authenticate()
            .test()
            .values()[0]
            .apply {
                this.userId `should be equal to` "User"
                this.token `should be equal to` "ABC"
            }
    }

    private fun nabuSessionTokenResponse(
        userId: String,
        token: String
    ): NabuSessionTokenResponse {
        return NabuSessionTokenResponse(
            id = "",
            userId = userId,
            token = token,
            isActive = true,
            expiresAt = "",
            insertedAt = "",
            updatedAt = ""
        )
    }

    private fun givenToken(userId: String, token: String): NabuToken =
        mock {
            on { fetchNabuToken() }.thenReturn(
                Single.just(
                    NabuOfflineTokenResponse(
                        userId,
                        token
                    )
                )
            )
        }
}
