package com.blockchain.nabu.analytics

import com.blockchain.api.analytics.AnalyticsContext
import com.blockchain.api.services.AnalyticsService
import com.blockchain.api.services.NabuAnalyticsEvent
import com.blockchain.nabu.datamanagers.analytics.AnalyticsContextProvider
import com.blockchain.nabu.datamanagers.analytics.AnalyticsLocalPersistence
import com.blockchain.nabu.datamanagers.analytics.NabuAnalytics
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.utils.Optional
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.androidcore.utils.PersistentPrefs

class NabuAnalyticsTest {
    private val localAnalyticsPersistence = mock<AnalyticsLocalPersistence>()

    private val token: Optional<NabuSessionTokenResponse> = Optional.Some(
        NabuSessionTokenResponse(
            "", "", "", true, "", "", ""
        )
    )
    private val tokenStore: NabuSessionTokenStore = mock {
        on { getAccessToken() }.thenReturn(Observable.just(token))
    }

    private val persistentPrefs: PersistentPrefs = mock {
        on { deviceId }.thenReturn("deviceID")
    }
    private val prefs: Lazy<PersistentPrefs> = mock {
        onGeneric { value }.thenReturn(persistentPrefs)
    }
    private val mockedContext: AnalyticsContext = mock()

    private val analyticsService = mock<AnalyticsService>()

    private val analyticsContextProvider: AnalyticsContextProvider = mock {
        on { context() }.thenReturn(mockedContext)
    }

    private val nabuAnalytics =
        NabuAnalytics(
            localAnalyticsPersistence = localAnalyticsPersistence, prefs = prefs,
            crashLogger = mock(), analyticsService = analyticsService, tokenStore = tokenStore,
            analyticsContextProvider = analyticsContextProvider
        )

    @Test
    fun flushIsWorking() {
        whenever(
            analyticsService.postEvents(
                events = any(),
                id = any(),
                analyticsContext = any(),
                platform = any(),
                device = any(),
                authorization = anyOrNull()
            )
        ).thenReturn(Completable.complete())

        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(84)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = nabuAnalytics.flush().test()

        testSubscriber.assertComplete()
        Mockito.verify(analyticsService, times(9))
            .postEvents(any(), any(), any(), any(), any(), anyOrNull())

        Mockito.verify(localAnalyticsPersistence, times(8)).removeOldestItems(10)
        Mockito.verify(localAnalyticsPersistence).removeOldestItems(4)
    }

    @Test
    fun flushOnEmptyStorageShouldNotInvokeAnyPosts() {
        whenever(
            analyticsService.postEvents(
                events = any(),
                id = any(),
                analyticsContext = any(),
                platform = any(),
                device = any(),
                authorization = anyOrNull()
            )
        ).thenReturn(Completable.complete())

        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(0)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = nabuAnalytics.flush().test()

        testSubscriber.assertComplete()
        Mockito.verify(analyticsService, never())
            .postEvents(any(), any(), any(), any(), any(), anyOrNull())

        Mockito.verify(localAnalyticsPersistence, never()).removeOldestItems(any())
    }

    @Test
    fun ifPostFailsCompletableShouldFailToo() {
        whenever(
            analyticsService.postEvents(
                events = any(),
                id = any(),
                analyticsContext = any(),
                platform = any(),
                device = any(),
                authorization = anyOrNull()
            )
        ).thenReturn(Completable.error(Throwable()))

        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(10)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = nabuAnalytics.flush().test()

        testSubscriber.assertNotComplete()
    }

    private fun randomListOfEventsWithSize(i: Int): List<NabuAnalyticsEvent> {
        return IntArray(i) { i }.map {
            NabuAnalyticsEvent(
                name = "name$it",
                type = "type$it",
                originalTimestamp = "originalTimestamp$it",
                properties = emptyMap(),
                numericProperties = emptyMap()
            )
        }
    }
}