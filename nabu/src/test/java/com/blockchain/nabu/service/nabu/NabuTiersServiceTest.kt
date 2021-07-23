package com.blockchain.nabu.service.nabu

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.FakeAuthenticator
import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.models.responses.nabu.TierUpdateJson
import com.blockchain.nabu.service.NabuTierService
import com.blockchain.nabu.util.fakefactory.FakeKycTiersFactory
import com.blockchain.nabu.util.fakefactory.FakeNabuSessionTokenFactory
import com.blockchain.nabu.util.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test

class NabuTiersServiceTest {

    private val sessionToken = FakeNabuSessionTokenFactory.any
    private val nabu: Nabu = mock()
    private val authenticator: FakeAuthenticator = FakeAuthenticator(sessionToken)

    private val subject: NabuTierService = NabuTierService(nabu, authenticator)

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
    }

    @Test
    fun `get tiers`() {
        val expectedTiers = FakeKycTiersFactory.any

        whenever(
            nabu.getTiers(sessionToken.authHeader)
        ).thenReturn(
            Single.just(expectedTiers)
        )

        subject.tiers()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it == expectedTiers
            }
    }

    @Test
    fun `set tier`() {
        val selectedTier = 1

        whenever(
            nabu.setTier(TierUpdateJson(selectedTier), sessionToken.authHeader)
        ).thenReturn(
            Completable.complete()
        )

        subject.setUserTier(selectedTier)
            .test()
            .waitForCompletionWithoutErrors()
    }
}
